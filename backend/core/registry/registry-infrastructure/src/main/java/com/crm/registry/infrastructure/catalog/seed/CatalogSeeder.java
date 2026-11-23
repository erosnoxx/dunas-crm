package com.crm.registry.infrastructure.catalog.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.infrastructure.seed.Seeder;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.domain.catalog.vos.TypeName;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Carga inicial do catálogo no boot: fabricantes, tipos e modelos de {@code seed/catalog.json}.
 *
 * <p>Só semeia quando o catálogo não tem nenhum fabricante nem tipo, então reiniciar o app não
 * recria nem sobrescreve nada. Tudo é cadastrado pelo domínio ({@code register}), na ordem
 * fabricantes, tipos, modelos; o modelo acha o fabricante e o tipo pelo nome, entre os que acabaram
 * de ser cadastrados na mesma carga. O arquivo nasce vazio ({@code []}): a lista do dono entra
 * depois, como dado.
 *
 * <p>Qualquer item recusado derruba o seed (e a transação do {@code DatabaseSeeder}) com uma
 * mensagem que diz qual item falhou, sem repetir o valor recusado.
 */
@RequiredArgsConstructor
public class CatalogSeeder implements Seeder<CatalogSeed> {

    private final ManufacturerRepository manufacturerRepository;
    private final EquipmentTypeRepository typeRepository;
    private final EquipmentModelRepository modelRepository;

    @Override
    public boolean isAlreadySeeded() {
        return manufacturerRepository.count() > 0 || typeRepository.count() > 0;
    }

    @Override
    public void seed(List<CatalogSeed> data) {
        for (var catalog : data)
            seed(catalog);
    }

    @Override
    public String resourcePath() {
        return "seed/catalog.json";
    }

    @Override
    public Class<CatalogSeed> recordType() {
        return CatalogSeed.class;
    }

    private void seed(CatalogSeed catalog) {
        var manufacturers = new HashMap<String, Manufacturer>();
        var types = new HashMap<String, EquipmentType>();

        for (var item : catalog.manufacturers()) {
            var manufacturer = guarded(
                    "fabricante " + quoted(item.name()),
                    () -> Manufacturer.register(ManufacturerName.of(item.name()))
            );

            manufacturerRepository.save(manufacturer);
            manufacturers.put(key(item.name()), manufacturer);
        }

        for (var item : catalog.types()) {
            var type = guarded("tipo " + quoted(item.name()), () -> registerType(item));

            typeRepository.save(type);
            types.put(key(item.name()), type);
        }

        for (var item : catalog.models())
            modelRepository.save(registerModel(item, manufacturers, types));
    }

    private static EquipmentType registerType(CatalogSeed.TypeSeed item) {
        return EquipmentType.register(
                TypeName.of(item.name()),
                CharacteristicName.of(item.characteristicName()),
                unitOf(item.unit()),
                item.required()
        );
    }

    private static EquipmentModel registerModel(
            CatalogSeed.ModelSeed item,
            Map<String, Manufacturer> manufacturers,
            Map<String, EquipmentType> types
    ) {
        var label = "modelo " + quoted(item.name());
        var manufacturer = manufacturers.get(key(item.manufacturer()));
        var type = types.get(key(item.type()));

        if (manufacturer == null)
            throw failed(label, "o fabricante não está na mesma carga", null);
        if (type == null)
            throw failed(label, "o tipo não está na mesma carga", null);

        return guarded(label, () -> EquipmentModel.register(
                manufacturer,
                type,
                ModelName.of(item.name()),
                List.of(new CharacteristicValue(
                        type.getCharacteristic().getId(),
                        CharacteristicAmount.of(item.amount())
                ))
        ));
    }

    /** A unidade é o nome exato do enum; texto que não é de nenhuma unidade é recusado. */
    private static MeasurementUnit unitOf(String raw) {
        return Arrays.stream(MeasurementUnit.values())
                .filter(unit -> unit.name().equals(raw))
                .findFirst()
                .orElseThrow(() -> new DomainException("unidade desconhecida"));
    }

    private static <T> T guarded(String label, Supplier<T> action) {
        try {
            return action.get();
        }
        catch (DomainException e) {
            throw failed(label, e.getMessage(), e);
        }
    }

    /** O nome do item identifica a falha; o valor recusado nunca vai para a mensagem. */
    private static InternalServerErrorException failed(String label, String reason, Throwable cause) {
        return new InternalServerErrorException("seed do catálogo: " + label + " recusado: " + reason, cause);
    }

    private static String quoted(String name) {
        return "\"" + name + "\"";
    }

    /** Referência por nome sem diferenciar maiúsculas, como a unicidade do banco. */
    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
