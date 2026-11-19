/** Ícone da DUNAS (docs/design-system/README.md, "Logo"): no tema escuro, só o ícone. */
function Logo({ size = 40 }: { size?: number }) {
  return (
    <img
      src="/logo.jpeg"
      alt="DUNAS"
      width={size}
      height={size}
      className="block rounded-lg"
      style={{ width: size, height: size }}
    />
  )
}

export { Logo }
