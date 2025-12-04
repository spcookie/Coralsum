import {describe, expect, it} from 'vitest'
import {createI18nInstance} from './index'

describe('i18n init', () => {
    it('should create instance with detected locale', async () => {
        const i18n = await createI18nInstance()
        expect(i18n.global.locale.value).toBeTypeOf('string')
        expect(['en', 'zh-CN', 'zh-TW']).toContain(i18n.global.locale.value)
    })
})

