package com.crm.web.iam.resolvers;

import com.crm.web.iam.attributes.ActorRequestAttribute;
import com.crm.common.application.exceptions.UnauthorizedException;
import com.crm.shared.application.Actor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Entrega o {@link Actor} da sessão autenticada ao parâmetro do controller.
 *
 * <p>Sem o atributo a requisição não passou pela autenticação (ou a rota ficou pública por
 * engano): responde 401 em vez de deixar o use case receber um ator nulo.
 */
public class ActorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Actor.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest request,
            WebDataBinderFactory binderFactory
    ) {
        var actor = request.getAttribute(ActorRequestAttribute.NAME, RequestAttributes.SCOPE_REQUEST);

        if (actor instanceof Actor resolved)
            return resolved;

        throw new UnauthorizedException("session_invalid", "sua sessão não vale mais; entre de novo");
    }
}
