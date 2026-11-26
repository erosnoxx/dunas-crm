import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../customers.service"
import type { CustomerDetail } from "../customers.service"
import { CustomerFormScreen } from "./customer-form"

vi.mock("../customers.service")

const SILVA: CustomerDetail = {
  id: "c2",
  document: "34218765000114",
  documentType: "CNPJ",
  name: "J. Silva Comércio de Alimentos Ltda",
  tradeName: "Padaria Boa Vista",
  email: "compras@padariaboavista.com.br",
  phone: "+5511999999999",
  status: "ACTIVE",
  locations: [],
}

const ANA: CustomerDetail = {
  ...SILVA,
  id: "c1",
  document: "31845726090",
  documentType: "CPF",
  name: "Ana Paula Ribeiro",
  tradeName: null,
  email: null,
  phone: "+551632124580",
}

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderForm(path: string, canEdit = true, cached?: CustomerDetail) {
  const client = createQueryClient(() => undefined)
  if (cached) client.setQueryData(["customers", "detail", cached.id], cached)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/clientes", Component: () => <p>lista de clientes</p> },
        {
          path: "/clientes/novo",
          Component: () => <CustomerFormScreen mode="new" canEdit={canEdit} />,
        },
        { path: "/clientes/:id", Component: () => <p>ficha do cliente</p> },
        {
          path: "/clientes/:id/editar",
          Component: () => <CustomerFormScreen mode="edit" canEdit={canEdit} />,
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

type User = ReturnType<typeof userEvent.setup>

const originalMatchMedia = window.matchMedia
const documentField = () => screen.getByLabelText("CPF ou CNPJ *")
const email = () => screen.getByLabelText("E-mail")
const phone = () => screen.getByLabelText("Telefone")
const contact = () =>
  screen.getByRole("region", {
    name: "Contato: informe pelo menos um, e-mail ou telefone",
  })
const save = (name = "Salvar cliente") => screen.getByRole("button", { name })

async function fillCompany(user: User) {
  await user.type(documentField(), "34218765000114")
  await user.type(screen.getByLabelText("Razão social *"), "J. Silva Comércio")
  await user.type(screen.getByLabelText("Nome fantasia (opcional)"), "Padaria Boa Vista")
  await user.type(phone(), "11999999999")
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.getCustomer).mockResolvedValue(SILVA)
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("novo cliente", () => {
  it("CNPJ digitado: tipo ao lado, máscara, Razão social e o nome fantasia", async () => {
    const user = renderForm("/clientes/novo")

    expect(screen.getByLabelText("Nome *")).toBeInTheDocument()
    expect(screen.queryByLabelText("Nome fantasia (opcional)")).not.toBeInTheDocument()

    await user.type(documentField(), "34218765000114")

    expect(documentField()).toHaveValue("34.218.765/0001-14")
    expect(screen.getByText("CNPJ")).toBeInTheDocument()
    expect(screen.getByLabelText("Razão social *")).toBeInTheDocument()
    expect(screen.getByLabelText("Nome fantasia (opcional)")).toBeInTheDocument()
    expect(
      screen.getByText(
        "Confira antes de salvar: o documento não pode ser alterado depois de salvo.",
      ),
    ).toBeInTheDocument()
    // CNPJ alfanumérico (Tarefa 1): o teclado é de texto; só o telefone é numérico.
    expect(documentField()).not.toHaveAttribute("inputmode")
    expect(phone()).toHaveAttribute("inputmode", "numeric")
  })

  it("os campos respeitam os limites do domínio", async () => {
    const user = renderForm("/clientes/novo")
    await user.type(documentField(), "34218765000114")

    expect(screen.getByLabelText("Razão social *")).toHaveAttribute("maxlength", "150")
    expect(screen.getByLabelText("Nome fantasia (opcional)")).toHaveAttribute(
      "maxlength",
      "150",
    )
    expect(email()).toHaveAttribute("maxlength", "254")
  })

  it("trocar para CPF esconde o nome fantasia e não envia o que estava nele", async () => {
    vi.mocked(service.registerCustomer).mockResolvedValue({ id: "c9" })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.clear(documentField())
    await user.type(documentField(), "31845726090")

    expect(screen.queryByLabelText("Nome fantasia (opcional)")).not.toBeInTheDocument()
    expect(screen.getByText("CPF")).toBeInTheDocument()
    expect(screen.getByLabelText("Nome *")).toHaveValue("J. Silva Comércio")

    await user.click(save())

    await waitFor(() =>
      expect(service.registerCustomer).toHaveBeenCalledWith(
        {
          document: "31845726090",
          name: "J. Silva Comércio",
          tradeName: null,
          email: null,
          phone: "+5511999999999",
        },
        expect.anything(),
      ),
    )
  })

  it("CPF com dígito errado mostra a mensagem sob o documento e não chama o serviço", async () => {
    const user = renderForm("/clientes/novo")
    await user.type(documentField(), "31845726091")
    await user.type(screen.getByLabelText("Nome *"), "Ana Paula Ribeiro")
    await user.type(email(), "ana@exemplo.com.br")

    await user.click(save())

    await waitFor(() =>
      expect(documentField()).toHaveAccessibleDescription(
        expect.stringContaining("CPF inválido: confira os números digitados."),
      ),
    )
    expect(documentField()).toHaveAttribute("aria-invalid", "true")
    expect(documentField()).toHaveFocus()
    expect(service.registerCustomer).not.toHaveBeenCalled()
  })

  it("sem contato: a mensagem no topo da seção Contato, e-mail e telefone marcados", async () => {
    const user = renderForm("/clientes/novo")
    await user.type(documentField(), "31845726090")
    await user.type(screen.getByLabelText("Nome *"), "Ana Paula Ribeiro")

    await user.click(save())

    const message = await within(contact()).findByText(
      "Informe pelo menos um contato: e-mail ou telefone.",
    )
    // No topo: antes dos dois campos.
    expect(
      message.compareDocumentPosition(email()) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
    expect(email()).toHaveAttribute("aria-invalid", "true")
    expect(phone()).toHaveAttribute("aria-invalid", "true")
    expect(email()).toHaveAccessibleDescription(
      "Informe pelo menos um contato: e-mail ou telefone.",
    )
    expect(email()).toHaveFocus()
    expect(service.registerCustomer).not.toHaveBeenCalled()
  })

  it('telefone "(11) 9999-999" mostra "Telefone inválido: informe DDD e número."', async () => {
    const user = renderForm("/clientes/novo")
    await user.type(documentField(), "31845726090")
    await user.type(screen.getByLabelText("Nome *"), "Ana Paula Ribeiro")
    await user.type(phone(), "119999999")
    expect(phone()).toHaveValue("(11) 9999-999")

    await user.click(save())

    await waitFor(() =>
      expect(phone()).toHaveAccessibleDescription(
        expect.stringContaining("Telefone inválido: informe DDD e número."),
      ),
    )
    expect(phone()).toHaveFocus()
    expect(service.registerCustomer).not.toHaveBeenCalled()
  })

  it("envia o documento normalizado, o telefone em +55 e o e-mail vazio como null, e abre a ficha nova", async () => {
    vi.mocked(service.registerCustomer).mockResolvedValue({ id: "c9" })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.click(save())

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.registerCustomer).toHaveBeenCalledWith(
      {
        document: "34218765000114",
        name: "J. Silva Comércio",
        tradeName: "Padaria Boa Vista",
        email: null,
        phone: "+5511999999999",
      },
      expect.anything(),
    )
  })

  it("409 customer_document_taken: mensagem e link sob o documento, o digitado fica e o foco vai ao documento", async () => {
    vi.mocked(service.registerCustomer).mockRejectedValue({
      status: 409,
      code: "customer_document_taken",
      message: "x",
    })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.click(save())

    await waitFor(() => expect(documentField()).toHaveFocus())
    expect(documentField()).toHaveAttribute("aria-invalid", "true")
    expect(documentField()).toHaveAccessibleDescription(
      expect.stringContaining("Já existe um cliente com este documento."),
    )
    expect(
      screen.getByRole("link", { name: "Ver cliente com este documento" }),
    ).toHaveAttribute("href", "/clientes?q=34218765000114&situacao=ALL")
    expect(documentField()).toHaveValue("34.218.765/0001-14")
    expect(screen.getByLabelText("Razão social *")).toHaveValue("J. Silva Comércio")
    expect(screen.getByLabelText("Nome fantasia (opcional)")).toHaveValue(
      "Padaria Boa Vista",
    )
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("contact_required da API sai no topo da seção Contato, com o foco no e-mail", async () => {
    vi.mocked(service.registerCustomer).mockRejectedValue({
      status: 422,
      code: "contact_required",
      message: "x",
    })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.click(save())

    expect(
      await within(contact()).findByText(
        "Informe pelo menos um contato: e-mail ou telefone.",
      ),
    ).toBeInTheDocument()
    expect(email()).toHaveAttribute("aria-invalid", "true")
    expect(phone()).toHaveAttribute("aria-invalid", "true")
    await waitFor(() => expect(email()).toHaveFocus())
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("trade_name_not_allowed sai sob o nome fantasia", async () => {
    vi.mocked(service.registerCustomer).mockRejectedValue({
      status: 422,
      code: "trade_name_not_allowed",
      message: "x",
    })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.click(save())

    const tradeName = screen.getByLabelText("Nome fantasia (opcional)")
    await waitFor(() => expect(tradeName).toHaveFocus())
    expect(tradeName).toHaveAccessibleDescription(
      expect.stringContaining("Nome fantasia só vale para pessoa jurídica."),
    )
  })

  it.each([
    [
      "concurrent_update",
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
    ["forbidden", "Você não tem permissão para isto."],
  ])("%s sai no aviso, sem perder o que foi digitado", async (code, text) => {
    vi.mocked(service.registerCustomer).mockRejectedValue({
      status: 409,
      code,
      message: "x",
    })
    const user = renderForm("/clientes/novo")
    await fillCompany(user)

    await user.click(save())

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(screen.getByLabelText("Razão social *")).toHaveValue("J. Silva Comércio")
  })

  it("Técnico: a rota de novo cliente volta para a lista", async () => {
    renderForm("/clientes/novo", false)

    expect(await screen.findByText("lista de clientes")).toBeInTheDocument()
  })

  it("celular: cabeçalho próprio com Voltar para a lista e Salvar no rodapé", () => {
    renderForm("/clientes/novo")

    expect(screen.getByRole("heading", { level: 1, name: "Novo cliente" })).toHaveClass(
      "text-title",
    )
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/clientes",
    )
    expect(save()).toHaveClass("min-h-12", "w-full")
    expect(screen.queryByRole("link", { name: "Cancelar" })).not.toBeInTheDocument()
  })

  it("web: título display, link Clientes, dicas e Cancelar", () => {
    setDesktop(true)
    renderForm("/clientes/novo")

    expect(screen.getByRole("heading", { level: 1, name: "Novo cliente" })).toHaveClass(
      "text-display-lg",
    )
    expect(screen.getByRole("link", { name: "Clientes" })).toHaveAttribute(
      "href",
      "/clientes",
    )
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes",
    )
    expect(screen.getByText(/^Digite o CPF ou o CNPJ/)).toBeInTheDocument()
    expect(screen.getByText("Fixo ou celular, com DDD.")).toBeInTheDocument()
    expect(save()).toHaveClass("md:font-semibold")
  })
})

describe("editar cliente", () => {
  it("o formulário só aparece com o cliente carregado; antes disso não há Salvar", async () => {
    vi.mocked(service.getCustomer).mockReturnValue(new Promise(() => undefined))
    renderForm("/clientes/c2/editar")

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar alterações" }),
    ).not.toBeInTheDocument()
    expect(service.updateCustomer).not.toHaveBeenCalled()
  })

  it("o cliente guardado no cache não preenche o formulário: só o lido agora", async () => {
    renderForm("/clientes/c2/editar", true, { ...SILVA, name: "Nome de antes" })

    expect(await screen.findByLabelText("Razão social *")).toHaveValue(
      "J. Silva Comércio de Alimentos Ltda",
    )
  })

  it("vem com os valores do cliente e o documento só leitura", async () => {
    renderForm("/clientes/c2/editar")

    expect(await screen.findByLabelText("Razão social *")).toHaveValue(
      "J. Silva Comércio de Alimentos Ltda",
    )
    expect(screen.getByLabelText("Nome fantasia (opcional)")).toHaveValue(
      "Padaria Boa Vista",
    )
    expect(email()).toHaveValue("compras@padariaboavista.com.br")
    expect(phone()).toHaveValue("(11) 99999-9999")
    expect(screen.getByText("34.218.765/0001-14")).toBeInTheDocument()
    expect(screen.getByText("CNPJ")).toBeInTheDocument()
    expect(
      screen.getByText("O documento não pode ser alterado depois de salvo."),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText("CPF ou CNPJ *")).not.toBeInTheDocument()
    expect(service.getCustomer).toHaveBeenCalledWith("c2")
  })

  it("Salvar alterações envia sem o documento e volta para a ficha", async () => {
    vi.mocked(service.updateCustomer).mockResolvedValue()
    const user = renderForm("/clientes/c2/editar")
    await user.clear(await screen.findByLabelText("E-mail"))

    await user.click(save("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateCustomer).toHaveBeenCalledWith("c2", {
      name: "J. Silva Comércio de Alimentos Ltda",
      tradeName: "Padaria Boa Vista",
      email: null,
      phone: "+5511999999999",
    })
  })

  it("CPF: rótulo Nome, sem nome fantasia, e envia tradeName null", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(ANA)
    vi.mocked(service.updateCustomer).mockResolvedValue()
    const user = renderForm("/clientes/c1/editar")

    expect(await screen.findByLabelText("Nome *")).toHaveValue("Ana Paula Ribeiro")
    expect(screen.queryByLabelText("Nome fantasia (opcional)")).not.toBeInTheDocument()
    expect(phone()).toHaveValue("(16) 3212-4580")

    await user.click(save("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateCustomer).toHaveBeenCalledWith("c1", {
        name: "Ana Paula Ribeiro",
        tradeName: null,
        email: null,
        phone: "+551632124580",
      }),
    )
  })

  it("telefone estrangeiro: como está, desabilitado, com a dica, e reenviado sem mudança", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, phone: "+14155552671" })
    vi.mocked(service.updateCustomer).mockResolvedValue()
    const user = renderForm("/clientes/c2/editar")

    const field = await screen.findByLabelText("Telefone")
    expect(field).toHaveValue("+14155552671")
    expect(field).toBeDisabled()
    expect(field).toHaveAccessibleDescription(
      "Número de fora do Brasil: não se edita nesta tela",
    )

    await user.click(save("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateCustomer).toHaveBeenCalledWith(
        "c2",
        expect.objectContaining({ phone: "+14155552671" }),
      ),
    )
  })

  it("cliente desativado redireciona para a ficha", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    renderForm("/clientes/c2/editar")

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })

  it("Técnico: a edição volta para a ficha", async () => {
    renderForm("/clientes/c2/editar", false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })

  it("customer_not_found sai no aviso", async () => {
    vi.mocked(service.getCustomer).mockRejectedValue({
      status: 404,
      code: "customer_not_found",
      message: "x",
    })
    renderForm("/clientes/c2/editar")

    expect(await screen.findByRole("alert")).toHaveTextContent("Cliente não encontrado.")
  })

  it("web: título Editar cliente, link e Cancelar para a ficha", async () => {
    setDesktop(true)
    renderForm("/clientes/c2/editar")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Editar cliente" }),
    ).toHaveClass("text-display-lg")
    expect(screen.getByRole("link", { name: "Ficha do cliente" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(await screen.findByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
  })
})
