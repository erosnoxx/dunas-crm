import type { ReactNode } from "react"
import { useFormContext, useWatch } from "react-hook-form"

import { cn } from "@/shared/lib/cn"
import { Button } from "@/shared/ui/button"
import { FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { PASSWORD_MAX } from "../password-rules"
import { PasswordChecklist } from "./password-checklist"
import { PasswordField } from "./password-field"

/**
 * Nova senha (contador e checklist) e repetição, dentro de um `<Form>`. `name` é o campo da
 * nova senha no schema da tela; o da repetição é sempre `repeat`.
 */
function NewPasswordFields({ name }: { name: "password" | "newPassword" }) {
  const { control, getValues, trigger } = useFormContext()
  const password = String(useWatch({ control, name }) ?? "")

  return (
    <>
      <FormField
        control={control}
        name={name}
        render={({ field }) => (
          <PasswordField
            {...field}
            id={name}
            label="Nova senha"
            autoComplete="new-password"
            showCount
            onChange={(event) => {
              field.onChange(event)
              // A repetição depende da senha: revalida para o erro não ficar velho.
              if (getValues("repeat")) void trigger("repeat")
            }}
          />
        )}
      />
      <PasswordChecklist password={password} />
      <FormField
        control={control}
        name="repeat"
        render={({ field }) => (
          <FormItem className="gap-1.5">
            <FormLabel>Repita a senha</FormLabel>
            <Input
              {...field}
              type="password"
              autoComplete="new-password"
              maxLength={PASSWORD_MAX}
            />
            <FormMessage />
          </FormItem>
        )}
      />
    </>
  )
}

/** Botão de envio das telas de senha: cinza e sem opacidade até o formulário estar pronto. */
function PasswordSubmitButton({
  ready,
  loading,
  children,
}: {
  ready: boolean
  loading: boolean
  children: ReactNode
}) {
  return (
    <Button
      type="submit"
      size="lg"
      loading={loading}
      disabled={!ready}
      className={cn(
        "md:min-h-12 md:cut-sm",
        !ready && "bg-line text-text-muted disabled:opacity-100",
      )}
    >
      {children}
    </Button>
  )
}

export { NewPasswordFields, PasswordSubmitButton }
