import { z } from "zod"

import { errorMessage, FORM_MESSAGES as M } from "./customers.messages"
import { UFS } from "./customers.service"
import { checkDocument, documentTypeOf } from "./document"
import { checkPhone } from "./phone"

// Limites de docs/system-design/03-customers/domain-model.md e mensagens da tabela "Mensagens da tela".
// Aparar as pontas é seguro: nenhum destes campos é senha.
const text = (
  required: string,
  length?: { min: number; max: number; message: string },
) => {
  const filled = z.string().trim().min(1, required)

  if (length === undefined) return filled

  return filled.min(length.min, length.message).max(length.max, length.message)
}

// O formato que o `Email` do backend aceita.
const EMAIL =
  /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$/

const isNameLength = (value: string) => value.length >= 2 && value.length <= 150

/**
 * O formulário do cliente. A mensagem do nome vazio depende do documento (nome ou razão social), e
 * a regra do contato é sobre dois campos: por isso a conferência é do objeto. O erro do contato vai
 * no caminho `contact`, para o topo da seção "Contato".
 */
export const customerSchema = z
  .object({
    document: z.string().trim(),
    name: z.string().trim(),
    tradeName: z.string().trim(),
    email: z.string().trim(),
    phone: z.string().trim(),
  })
  .superRefine((values, context) => {
    const issue = (path: string, message: string) =>
      context.addIssue({ code: "custom", path: [path], message })
    const document = checkDocument(values.document)
    const isCompany = documentTypeOf(values.document) === "CNPJ"

    if (!document.ok) issue("document", document.message)

    if (values.name === "")
      issue("name", isCompany ? M.corporateNameRequired : M.nameRequired)
    if (values.name !== "" && !isNameLength(values.name)) issue("name", M.nameLength)

    // Nome fantasia só com CNPJ (ADR-21): a tela nem mostra o campo para CPF.
    if (values.tradeName !== "" && !isCompany)
      issue("tradeName", errorMessage({ code: "trade_name_not_allowed" }))
    if (values.tradeName !== "" && isCompany && !isNameLength(values.tradeName))
      issue("tradeName", M.nameLength)

    if (values.email !== "" && !EMAIL.test(values.email)) issue("email", M.emailInvalid)
    if (values.phone !== "" && !checkPhone(values.phone)) issue("phone", M.phoneInvalid)
    if (values.email === "" && values.phone === "") issue("contact", M.contactRequired)
  })

export const locationSchema = z.object({
  name: text(M.locationNameRequired, { min: 2, max: 60, message: M.locationNameLength }),
  zipCode: text(M.zipCodeRequired).refine(
    (value) => value.replace(/\D/g, "").length === 8,
    M.zipCodeIncomplete,
  ),
  street: text(M.streetRequired, { min: 2, max: 150, message: M.streetLength }),
  number: text(M.numberRequired),
  complement: z.string().trim(),
  neighborhood: text(M.neighborhoodRequired, {
    min: 2,
    max: 100,
    message: M.neighborhoodLength,
  }),
  city: text(M.cityRequired),
  state: z.enum(UFS, { error: M.stateRequired }),
})

/** `today` (`yyyy-MM-dd`) vem de quem chama: o relógio fica fora do schema e o teste, determinístico. */
export const equipmentSchema = (today: string) =>
  z.object({
    modelId: z.string().min(1, M.modelRequired),
    serialNumber: text(M.serialNumberRequired),
    installedOn: z.string().refine((value) => value <= today, M.installedOnFuture),
  })

export type CustomerValues = z.infer<typeof customerSchema>
export type LocationValues = z.infer<typeof locationSchema>
export type EquipmentValues = z.infer<ReturnType<typeof equipmentSchema>>
