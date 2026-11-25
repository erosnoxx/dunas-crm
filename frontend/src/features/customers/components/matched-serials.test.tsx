import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { MatchedSerials } from "./matched-serials"

describe("MatchedSerials", () => {
  it("uma linha 'Série X · modelo' por série casada", () => {
    render(
      <MatchedSerials
        items={[
          { serialNumber: "HV2240187563", modelName: "SUN2000-5KTL-L1" },
          { serialNumber: "HV2290001234", modelName: "SUN2000-8KTL-M1" },
        ]}
      />,
    )

    expect(screen.getByText("Série HV2240187563 · SUN2000-5KTL-L1")).toHaveClass(
      "text-body-sm",
    )
    expect(screen.getByText("Série HV2290001234 · SUN2000-8KTL-M1")).toBeInTheDocument()
  })

  it("sem série casada, nada", () => {
    const { container } = render(<MatchedSerials items={[]} />)

    expect(container).toBeEmptyDOMElement()
  })
})
