import { mount } from '@vue/test-utils'
import Tags from '@/components/Tags.vue'

describe('Tags', () => {
  let propsData
  let wrapper
  const createWrapper = () => {
    wrapper = mount(Tags, { propsData })
  }

  beforeEach(() => {
    propsData = {}
  })

  afterEach(() => {
    wrapper.destroy()
    wrapper = null
  })

  describe('when tags prop is not set', () => {
    test('renders nothing', () => {
      createWrapper()
      expect(wrapper.html()).toEqual(``)
    })
  })

  describe('when tags prop is set to an empty array', () => {
    beforeEach(() => {
      propsData.tags = []
    })

    test('renders nothing', () => {
      createWrapper()
      expect(wrapper.html()).toEqual(``)
    })
  })

  describe('when tags prop is set to one tag', () => {
    beforeEach(() => {
      propsData.tags = ['test-tag-1']
    })

    test('renders an unordered list showing the tag', () => {
      createWrapper()
      expect(wrapper.html()).toMatchSnapshot()
    })
  })

  describe('when tags prop is set to multiple tags', () => {
    beforeEach(() => {
      propsData.tags = ['test-tag-1', 'test-tag-2', 'test-tag-3']
    })

    test('renders an unordered list showing the tags', () => {
      createWrapper()
      expect(wrapper.html()).toMatchSnapshot()
    })
  })
})
