package com.crm.shared.domain.enums;

/**
 * Unidades federativas do Brasil, pela sigla.
 */
public enum UF {
    AC("acre"), AL("Alagoas"), AP("Amapá"),
    AM("Amazonas"), BA("Bahia"), CE("Ceará"),
    DF("Distrito Federal"), ES("Espírito Santo"), GO("Goias"),
    MA("Maranhão"), MT("Mato Grosso"), MS("Mato Grosso do Sul"),
    MG("Minas Gerais"), PA("Pará"), PB("Paraíba"),
    PR("Paraná"), PE("Pernambuco"), PI("Piauí"),
    RJ("Rio de Janeiro"), RN("Rio Grande do Norte"), RS("Rio Grande do Sul"),
    RO("Rondônia"), RR("Roraima"), SC("Santa Catarina"),
    SP("São Paulo"), SE("Sergipe"), TO("Tocantins");

    private final String name;

    UF(String stateName) {
        this.name = stateName;
    }
}
