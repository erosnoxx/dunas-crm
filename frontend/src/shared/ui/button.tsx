import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { Loader2Icon } from "lucide-react"

import { cn } from "@/shared/lib/cn"

/**
 * Botão da DUNAS (docs/design-system/README.md). O principal é laranja com texto `on-brand`,
 * um por tela; o secundário tem contorno `line-strong` (duas camadas, porque `clip-path` corta
 * a borda). O corte do canto é o do logo: `cut-sm` no `md`, `cut-md` no `lg` (celular).
 */
const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 font-sans font-semibold whitespace-nowrap select-none disabled:pointer-events-none disabled:opacity-50 data-[loading=true]:pointer-events-none [&_svg]:pointer-events-none [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        primary: "bg-brand text-on-brand hover:brightness-110",
        outline: "bg-line-strong p-px text-text",
        "danger-text": "bg-transparent text-danger-text hover:underline",
        link: "bg-transparent text-brand-text underline hover:brightness-110",
      },
      size: {
        md: "min-h-11 px-5 text-base",
        lg: "min-h-13 px-6 text-base",
      },
    },
    compoundVariants: [
      { variant: ["primary", "outline"], size: "md", className: "cut-sm" },
      { variant: ["primary", "outline"], size: "lg", className: "cut-md" },
      { variant: ["danger-text", "link"], className: "px-0" },
    ],
    defaultVariants: { variant: "primary", size: "md" },
  },
)

type ButtonProps = ButtonPrimitive.Props &
  VariantProps<typeof buttonVariants> & {
    loading?: boolean
  }

function Button({
  className,
  variant = "primary",
  size = "md",
  loading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  const content = (
    <>
      {loading && <Loader2Icon className="size-4 animate-spin" aria-hidden="true" />}
      {children}
    </>
  )

  return (
    <ButtonPrimitive
      data-slot="button"
      data-loading={loading}
      aria-busy={loading || undefined}
      disabled={disabled || loading}
      focusableWhenDisabled={loading}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    >
      {variant === "outline" ? (
        <span
          className={cn(
            "inline-flex min-h-[inherit] flex-1 items-center justify-center gap-2 bg-ground px-5",
            size === "lg" ? "cut-md" : "cut-sm",
          )}
        >
          {content}
        </span>
      ) : (
        content
      )}
    </ButtonPrimitive>
  )
}

export { Button }
