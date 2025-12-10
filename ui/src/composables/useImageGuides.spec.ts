import {describe, expect, it} from 'vitest'
import {useImageGuides} from './useImageGuides'

describe('useImageGuides', () => {
    it('init creates equal guides', () => {
        const g = useImageGuides()
        g.init(100, 80, 5, 4)
        expect(g.xGuides.value).toEqual([20, 40, 60, 80])
        expect(g.yGuides.value).toEqual([20, 40, 60])
    })

    it('clamp keeps guides ordered', () => {
        const g = useImageGuides()
        g.init(100, 100, 3, 3)
        g.clampGuideX(0, 90)
        expect(g.xGuides.value[0]).toBeLessThan(g.xGuides.value[1])
        g.clampGuideY(1, 5)
        expect(g.yGuides.value[0]).toBeLessThan(g.yGuides.value[1])
    })

    it('rects shape matches rows*cols', () => {
        const g = useImageGuides()
        g.init(100, 50, 2, 5)
        const rows = g.rects()
        expect(rows.length).toBe(5)
        expect(rows[0].length).toBe(2)
    })
})

