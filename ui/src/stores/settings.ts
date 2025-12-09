import {defineStore} from 'pinia'

export type AspectRatio = 'auto' | '1:1' | '2:3' | '3:2' | '3:4' | '4:3' | '9:16' | '16:9' | '21:9'
export type ResolutionScale = '1x' | '2x' | '3x' | '4x'
export type OutputFormat = 'PNG' | 'JPG'
export type ImageSize = '1K' | '2K' | '4K'
export type MediaResolution = 'auto' | 'low' | 'medium' | 'high'
export type ModelType = 'Basic' | 'Pro'

export const useSettingsStore = defineStore('settings', {
    state: () => ({
        candidateRadio: 1 as 1 | 2 | 3 | 4,
        aspectRatio: 'auto' as AspectRatio,
        candidateSlider: 0,
        topP: 0.95,
        temperature: 1.0,
        resolution: '1x' as ResolutionScale,
        outputFormat: 'PNG' as OutputFormat,
        imageSize: '1K' as ImageSize,
        mediaResolution: 'auto' as MediaResolution,
        modelType: 'Basic' as ModelType,
        darkMode: false,
        advancedExpanded: false,
        systemPrompt: '' as string,
        basicExpandedNames: [] as Array<string | number>
    }),
    actions: {
        toggleDark() {
            this.darkMode = !this.darkMode
        },
        setSystemPrompt(v: string) {
            const s = (v || '').trim()
            this.systemPrompt = s
            const hasSys = this.basicExpandedNames.includes('sys')
            if (s.length > 0) {
                if (!hasSys) this.basicExpandedNames = [...this.basicExpandedNames, 'sys']
            } else {
                if (hasSys) this.basicExpandedNames = this.basicExpandedNames.filter((n) => n !== 'sys')
            }
        },
        setBasicExpandedNames(names: Array<string | number>) {
            this.basicExpandedNames = Array.isArray(names) ? names : []
        },
        reset() {
            this.candidateRadio = 1
            this.aspectRatio = 'auto'
            this.candidateSlider = 0
            this.topP = 0.95
            this.temperature = 1.0
            this.resolution = '1x'
            this.outputFormat = 'PNG'
            this.imageSize = '1K'
            this.mediaResolution = 'auto'
            this.modelType = 'Basic'
            this.systemPrompt = ''
            this.basicExpandedNames = []
        }
    }
})
