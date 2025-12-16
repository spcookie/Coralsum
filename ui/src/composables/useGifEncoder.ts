import gifenc, {applyPalette, quantize} from 'gifenc'

export interface GifOptions {
    fps: number
    loop?: number
    quality?: number
}

export function useGifEncoder() {
    function encodeGif(
        frames: HTMLCanvasElement[],
        options: GifOptions,
        onProgress?: (percent: number) => void
    ): Promise<Blob> {
        return new Promise((resolve, reject) => {
            if (frames.length === 0) {
                reject(new Error('No frames to encode'))
                return
            }

            try {
                const {fps, loop = 0, quality = 10} = options
                const delay = Math.round(1000 / fps)

                // Create GIF encoder
                const gif = gifenc()

                // Process each frame
                frames.forEach((canvas, index) => {
                    // Report progress
                    if (onProgress) {
                        onProgress(Math.round(((index + 1) / frames.length) * 100))
                    }

                    // Get context with willReadFrequently set
                    const ctx = canvas.getContext('2d', {willReadFrequently: true})
                    if (!ctx) {
                        throw new Error('Failed to get canvas context')
                    }

                    // Get image data
                    const width = canvas.width
                    const height = canvas.height
                    const imageData = ctx.getImageData(0, 0, width, height)
                    const {data} = imageData

                    // Quantize colors to create palette (256 colors max)
                    // Quality parameter affects number of colors - higher quality = more colors
                    const numColors = Math.min(256, Math.max(2, Math.floor(256 * (quality / 10))))
                    const palette = quantize(data, numColors)

                    // Apply palette to get indexed pixels
                    const indexPixels = applyPalette(data, palette)

                    // Write frame
                    const frameOptions: any = {
                        palette,
                        delay
                    }

                    // For first frame, add repeat count
                    if (index === 0) {
                        frameOptions.repeat = loop
                    }

                    gif.writeFrame(indexPixels, width, height, frameOptions)
                })

                // Finish GIF
                gif.finish()

                // Convert to blob
                const buffer = gif.bytes()
                const blob = new Blob([buffer], {type: 'image/gif'})
                resolve(blob)
            } catch (err) {
                reject(err)
            }
        })
    }

    return {
        encodeGif
    }
}