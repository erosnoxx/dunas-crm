package com.crm.web.registry;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import com.crm.registry.application.customers.usecases.commands.DeactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.ReactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.RegisterCustomer;
import com.crm.registry.application.customers.usecases.commands.UpdateCustomer;
import com.crm.registry.application.customers.usecases.queries.GetCustomer;
import com.crm.registry.application.customers.usecases.queries.ListCustomers;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.RegisterCustomerRequest;
import com.crm.web.registry.requests.UpdateCustomerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Clientes. A checagem de papel é do use case, nunca daqui.
 *
 * <p>O filtro da lista é ligado direto de {@code text} e {@code status}: o Spring converte o texto
 * no enum, então o controller não conhece o domínio e valor desconhecido vira 400.
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomersController {

    private static final int MAX_PAGE_SIZE = 100;

    private final RegisterCustomer registerCustomer;
    private final UpdateCustomer updateCustomer;
    private final DeactivateCustomer deactivateCustomer;
    private final ReactivateCustomer reactivateCustomer;
    private final ListCustomers listCustomers;
    private final GetCustomer getCustomer;

    @PostMapping
    public ResponseEntity<UUIDResponse> register(
            Actor actor,
            @Valid @RequestBody RegisterCustomerRequest body
    ) {
        var created = registerCustomer.execute(new RegisterCustomer.Params(
                actor,
                body.document(),
                body.name(),
                body.tradeName(),
                body.email(),
                body.phone()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<CustomerSummary> list(
            Actor actor,
            @ModelAttribute CustomerFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = Pageable.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return listCustomers.execute(new ListCustomers.Params(
                actor,
                filter,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public CustomerDetail detail(
            Actor actor,
            @PathVariable UUID id
    ) {
        return getCustomer.execute(new GetCustomer.Params(actor, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest body
    ) {
        updateCustomer.execute(new UpdateCustomer.Params(
                actor,
                id,
                body.name(),
                body.tradeName(),
                body.email(),
                body.phone()
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateCustomer.execute(new DeactivateCustomer.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateCustomer.execute(new ReactivateCustomer.Params(actor, id));

        return ResponseEntity.noContent().build();
    }
}
