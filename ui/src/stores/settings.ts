import {defineStore} from 'pinia'

export type AspectRatio = 'auto' | '1:1' | '2:3' | '3:2' | '3:4' | '4:3' | '9:16' | '16:9' | '21:9'
export type ResolutionScale = '1x' | '2x' | '4x'
export type OutputFormat = 'PNG' | 'JPG' | 'WEBP'

export const useSettingsStore = defineStore('settings', {
    state: () => ({
        candidateRadio: 1 as 1 | 2 | 3 | 4,
        aspectRatio: 'auto' as AspectRatio,
        candidateSlider: 0,
        topP: 1.0,
        temperature: 0.7,
        resolution: '1x' as ResolutionScale,
        outputFormat: 'PNG' as OutputFormat,
        darkMode: false
    }),
    actions: {
        toggleDark() {
            this.darkMode = !this.darkMode
        },
        reset() {
            this.candidateRadio = 1
            this.aspectRatio = 'auto'
            this.candidateSlider = 0
            this.topP = 1.0
            this.temperature = 0.7
            this.resolution = '1x'
            this.outputFormat = 'PNG'
        }
    }
})