import {ref} from 'vue'

export interface GuidesState {
    imageWidth: number
    imageHeight: number
    xGuides: number[]
    yGuides: number[]
}

export function useImageGuides() {
    const imageWidth = ref(0)
    const imageHeight = ref(0)
    const xGuides = ref<number[]>([])
    const yGuides = ref<number[]>([])

    function init(w: number, h: number, cols: number, rows: number) {
        imageWidth.value = w
        imageHeight.value = h
        const xs: number[] = []
        const ys: number[] = []
        for (let c = 1; c < cols; c++) xs.push(Math.round((w * c) / cols))
        for (let r = 1; r < rows; r++) ys.push(Math.round((h * r) / rows))
        xGuides.value = xs
        yGuides.value = ys
    }

    function clampGuideX(idx: number, x: number) {
        const min = idx === 0 ? 1 : xGuides.value[idx - 1] + 1
        const max = idx === xGuides.value.length - 1 ? imageWidth.value - 1 : xGuides.value[idx + 1] - 1
        xGuides.value[idx] = Math.min(Math.max(x, min), max)
    }

    function clampGuideY(idx: number, y: number) {
        const min = idx === 0 ? 1 : yGuides.value[idx - 1] + 1
        const max = idx === yGuides.value.length - 1 ? imageHeight.value - 1 : yGuides.value[idx + 1] - 1
        yGuides.value[idx] = Math.min(Math.max(y, min), max)
    }

    function rects(): Array<{ x: number; y: number; w: number; h: number }>[] {
        const xs = [0, ...xGuides.value, imageWidth.value]
        const ys = [0, ...yGuides.value, imageHeight.value]
        const out: Array<{ x: number; y: number; w: number; h: number }>[] = []
        for (let r = 0; r < ys.length - 1; r++) {
            const row: { x: number; y: number; w: number; h: number }[] = []
            for (let c = 0; c < xs.length - 1; c++) {
                row.push({x: xs[c], y: ys[r], w: xs[c + 1] - xs[c], h: ys[r + 1] - ys[r]})
            }
            out.push(row)
        }
        return out
    }

    return {imageWidth, imageHeight, xGuides, yGuides, init, clampGuideX, clampGuideY, rects}
}

