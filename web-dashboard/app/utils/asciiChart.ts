// Simpan gambar sementara supaya grafik bisa ikut animasi ASCII.
export async function snapshotAsciiChart(svg: SVGSVGElement): Promise<{ remove: () => void } | null> {
  const bounds = svg.getBoundingClientRect()
  const parent = svg.parentElement
  if (!bounds.width || !bounds.height || !parent) return null

  const clone = svg.cloneNode(true) as SVGSVGElement
  const sources = [svg, ...svg.querySelectorAll('*')]
  const copies = [clone, ...clone.querySelectorAll('*')]
  const properties = ['fill', 'stroke', 'stroke-width', 'stroke-opacity', 'fill-opacity', 'stop-color', 'stop-opacity', 'opacity', 'color']
  sources.forEach((source, index) => {
    const style = getComputedStyle(source)
    for (const property of properties) {
      const value = style.getPropertyValue(property).replace(/url\(["']?[^#)]*#([^"')]+)["']?\)/g, 'url(#$1)')
      copies[index]?.setAttribute(property, value)
    }
  })
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  clone.setAttribute('width', String(bounds.width))
  clone.setAttribute('height', String(bounds.height))
  const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(clone)], { type: 'image/svg+xml' }))

  try {
    const image = new Image()
    image.src = url
    await image.decode()
    const canvas = document.createElement('canvas')
    const scale = Math.min(window.devicePixelRatio || 1, 2)
    canvas.width = Math.ceil(bounds.width * scale)
    canvas.height = Math.ceil(bounds.height * scale)
    const context = canvas.getContext('2d')
    if (!context || !svg.isConnected) return null
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    canvas.setAttribute('aria-hidden', 'true')
    canvas.setAttribute('data-ascii-chart-snapshot', '')

    const parentBounds = parent.getBoundingClientRect()
    const previousPosition = parent.style.position
    const previousVisibility = svg.style.visibility
    parent.style.position = 'relative'
    canvas.style.position = 'absolute'
    canvas.style.left = `${bounds.left - parentBounds.left}px`
    canvas.style.top = `${bounds.top - parentBounds.top}px`
    canvas.style.width = `${bounds.width}px`
    canvas.style.height = `${bounds.height}px`
    canvas.style.pointerEvents = 'none'
    parent.appendChild(canvas)
    svg.style.visibility = 'hidden'
    return {
      remove() {
        canvas.remove()
        svg.style.visibility = previousVisibility
        parent.style.position = previousPosition
      }
    }
  } finally {
    URL.revokeObjectURL(url)
  }
}
