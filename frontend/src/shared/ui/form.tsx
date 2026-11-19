import * as React from "react"
import { Field } from "@base-ui/react/field"
import {
  Controller,
  FormProvider,
  useFormContext,
  useFormState,
  type ControllerProps,
  type FieldPath,
  type FieldValues,
} from "react-hook-form"

import { cn } from "@/shared/lib/cn"
import { Label } from "@/shared/ui/label"

const Form = FormProvider

type FormFieldContextValue<
  TFieldValues extends FieldValues = FieldValues,
  TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
> = {
  name: TName
}

const FormFieldContext = React.createContext<FormFieldContextValue>(
  {} as FormFieldContextValue,
)

function FormField<
  TFieldValues extends FieldValues = FieldValues,
  TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
>(props: ControllerProps<TFieldValues, TName>) {
  return (
    <FormFieldContext.Provider value={{ name: props.name }}>
      <Controller {...props} />
    </FormFieldContext.Provider>
  )
}

function useFormField() {
  const fieldContext = React.useContext(FormFieldContext)
  const { getFieldState } = useFormContext()
  const formState = useFormState({ name: fieldContext.name })
  const fieldState = getFieldState(fieldContext.name, formState)

  return { name: fieldContext.name, ...fieldState }
}

function FormItem({ className, ...props }: React.ComponentProps<typeof Field.Root>) {
  const { invalid } = useFormField()

  return (
    <Field.Root
      data-slot="form-item"
      invalid={invalid}
      className={cn("grid gap-2", className)}
      {...props}
    />
  )
}

function FormLabel(props: React.ComponentProps<typeof Field.Label>) {
  return <Field.Label render={<Label />} data-slot="form-label" {...props} />
}

function FormDescription({
  className,
  ...props
}: React.ComponentProps<typeof Field.Description>) {
  return (
    <Field.Description
      data-slot="form-description"
      className={cn("text-caption text-text-muted", className)}
      {...props}
    />
  )
}

/** `Field.Error` com `match`: sempre visível quando há texto, e no `aria-describedby` do campo. */
function FormMessage({
  className,
  children,
  ...props
}: React.ComponentProps<typeof Field.Error>) {
  const { error } = useFormField()
  const body = error ? String(error?.message ?? "") : children

  if (!body) {
    return null
  }

  return (
    <Field.Error
      match
      render={<p />}
      data-slot="form-message"
      className={cn("text-caption text-danger-text", className)}
      {...props}
    >
      {body}
    </Field.Error>
  )
}

export {
  useFormField,
  Form,
  FormItem,
  FormLabel,
  FormDescription,
  FormMessage,
  FormField,
}
