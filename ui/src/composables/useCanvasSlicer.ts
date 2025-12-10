export async function sliceImageToDataUrls(img: HTMLImageElement, rects: Array<{
    x: number;
    y: number;
    w: number;
    h: number
}>[]): Promise<string[]> {
    const urls: string[] = []
    for (const r of rects) {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) throw new Error('no canvas ctx')
        canvas.width = r.w
        canvas.height = r.h
        ctx.drawImage(img, r.x, r.y, r.w, r.h, 0, 0, r.w, r.h)
        urls.push(canvas.toDataURL('image/png'))
    }
    return urls
}

export function buildZipFileName(row: number, col: number) {
    return `slice_r${row}_c${col}.png`
}

