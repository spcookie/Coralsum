export interface SpriteConfig {
    frameWidth: number
    frameHeight: number
    startFrame: number
    endFrame: number
}

export interface Frame {
    canvas: HTMLCanvasElement
    index: number
}

export function useSpriteExtractor() {
    function extractFrames(img: HTMLImageElement, config: SpriteConfig): Frame[] {
        const {frameWidth, frameHeight, startFrame, endFrame} = config
        const frames: Frame[] = []

        const imgWidth = img.naturalWidth || img.width
        const imgHeight = img.naturalHeight || img.height

        const cols = Math.floor(imgWidth / frameWidth)
        const rows = Math.floor(imgHeight / frameHeight)
        const totalFrames = cols * rows

        const start = Math.max(0, startFrame)
        const end = Math.min(totalFrames - 1, endFrame)

        for (let i = start; i <= end; i++) {
            const row = Math.floor(i / cols)
            const col = i % cols

            const sx = col * frameWidth
            const sy = row * frameHeight

            const canvas = document.createElement('canvas')
            canvas.width = frameWidth
            canvas.height = frameHeight
            const ctx = canvas.getContext('2d', {willReadFrequently: true})

            if (!ctx) {
                throw new Error('Failed to get canvas context')
            }

            ctx.drawImage(
                img,
                sx, sy, frameWidth, frameHeight,
                0, 0, frameWidth, frameHeight
            )

            frames.push({canvas, index: i})
        }

        return frames
    }

    function extractFramesFromRects(
        img: HTMLImageElement,
        rects: Array<{ x: number; y: number; w: number; h: number }>,
        startFrame: number,
        endFrame: number
    ): Frame[] {
        const frames: Frame[] = []
        const start = Math.max(0, startFrame)
        const end = Math.min(rects.length - 1, endFrame)

        for (let i = start; i <= end; i++) {
            const r = rects[i]
            const canvas = document.createElement('canvas')
            canvas.width = r.w
            canvas.height = r.h
            const ctx = canvas.getContext('2d', {willReadFrequently: true})

            if (!ctx) {
                throw new Error('Failed to get canvas context')
            }

            ctx.drawImage(
                img,
                r.x, r.y, r.w, r.h,
                0, 0, r.w, r.h
            )

            frames.push({canvas, index: i})
        }

        return frames
    }

    function calculateGridInfo(imgWidth: number, imgHeight: number, frameWidth: number, frameHeight: number) {
        const cols = Math.floor(imgWidth / frameWidth)
        const rows = Math.floor(imgHeight / frameHeight)
        const totalFrames = cols * rows

        return {cols, rows, totalFrames}
    }

    return {
        extractFrames,
        extractFramesFromRects,
        calculateGridInfo
    }
}