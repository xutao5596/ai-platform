import { HtmlNode, HtmlNodeModel } from '@logicflow/core'
import type { NodeDefinition } from '@/types/flow'

/**
 * Build an HTML view + model pair for a given node definition.
 * Style: Jeecg-AI inspired. 332 x auto height, white background, no border,
 * colored icon + label. Anchors are simple left/right circles.
 */
export function buildNodeViewModel(def: NodeDefinition) {
  const t = def.typeKey

  class Model extends HtmlNodeModel {
    setAttributes() {
      this.width = this.properties.width || 332
      this.height = this.properties.height || 62
      // Start nodes: only right anchor; End nodes: only left anchor
      if (t === 'start') {
        this.sourceRules.push({ message: '开始节点不能作为连线终点', validate: () => false })
      }
      if (t === 'end') {
        this.targetRules.push({ message: '结束节点不能作为连线起点', validate: () => false })
      }
    }
    getDefaultAnchor() {
      if (t === 'start') {
        return [{ x: this.width / 2, y: this.height, type: 'bottom' }]
      }
      if (t === 'end') {
        return [{ x: this.width / 2, y: 0, type: 'top' }]
      }
      return [
        { x: 0, y: this.height / 2, type: 'left' },
        { x: this.width, y: this.height / 2, type: 'right' }
      ]
    }
    getNodeStyle() {
      return {
        fill: '#ffffff',
        stroke: 'none',
        strokeWidth: 0,
        cursor: 'move'
      }
    }
    getTextStyle() {
      return { color: 'transparent', fontSize: 0, background: { fill: 'none' } }
    }
    getAnchorStyle() {
      return { r: 4, fill: '#ffffff', stroke: '#909399', strokeWidth: 1.5 }
    }
    getAnchorLineStyle() {
      return { stroke: '#909399', strokeWidth: 1.5, strokeDasharray: '4 4' }
    }
    getOutlineStyle() {
      return { stroke: def.color || '#409EFF', strokeWidth: 2, strokeDasharray: '0', fill: 'none' }
    }
  }

  class View extends HtmlNode {
    setHtml(rootEl: SVGForeignObjectElement) {
      const props = (this.props.model.properties || {}) as Record<string, any>
      const color = def.color || '#409EFF'
      const status = props._runStatus as string | undefined
      const statusColor = status === 'success' ? '#52c41a'
        : status === 'failed' ? '#f5222d'
        : status === 'running' ? '#67b7ff'
        : null
      const highlight = props._highlight ? 'box-shadow: 0 0 0 2px #409EFF;' : ''
      const dim = (props._highlight === false) ? 'opacity: 0.45;' : ''
      const label = escapeHtml(props._label || def.displayName)
      const sub = escapeHtml(props._sub || def.description || '')
      const iconSvg = iconFor(def.typeKey, color)
      const runTag = status ? `<div class="lf-run-status ${status}" style="position:absolute;top:6px;right:8px;font-size:10px;padding:1px 6px;border-radius:8px;background:${statusColor};color:#fff;">${status === 'success' ? '成功' : status === 'failed' ? '失败' : status === 'running' ? '运行中' : status}</div>` : ''
      // De-dupe prior content we created. (Preact's reconciler leaves the
      // foreignObject's children alone between renders, but our own setHtml
      // gets called multiple times.)
      while (rootEl.firstChild) rootEl.removeChild(rootEl.firstChild)
      const wrap = document.createElement('div')
      wrap.className = 'ai-node'
      wrap.style.cssText = `position:relative;width:${this.props.model.width}px;min-height:${this.props.model.height}px;box-sizing:border-box;border-radius:8px;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,0.08),0 1px 2px rgba(0,0,0,0.06);${highlight}${dim}`
      wrap.innerHTML = `
        ${runTag}
        <div class="ai-node-header" style="display:flex;align-items:center;padding:10px 12px;gap:8px;">
          <div class="ai-node-icon" style="width:28px;height:28px;border-radius:6px;display:flex;align-items:center;justify-content:center;background:${color}14;color:${color};flex-shrink:0;">${iconSvg}</div>
          <div class="ai-node-text" style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
            <div class="ai-node-label" style="font-size:13px;font-weight:600;color:#303133;line-height:1.2;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${label}</div>
            <div class="ai-node-desc" style="font-size:11px;color:#909399;line-height:1.2;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${sub}</div>
          </div>
        </div>
      `
      rootEl.appendChild(wrap)
    }
    shouldUpdate() {
      const cur = JSON.stringify(this.props.model.properties || {})
      if (this.currentProperties === undefined) {
        this.currentProperties = cur
        this.preProperties = cur
        return true
      }
      if (cur !== this.currentProperties) {
        this.preProperties = this.currentProperties
        this.currentProperties = cur
        return true
      }
      return false
    }
    componentDidMount() {
      // The HtmlNode base class also calls setHtml here, but we go through
      // setHtml directly. rootEl getter is wired by HtmlNode so Preact's
      // ref is what we need.
      const fo: SVGForeignObjectElement | null = (this as any).rootEl || null
      if (fo) this.setHtml(fo)
    }
    componentDidUpdate() {
      const fo: SVGForeignObjectElement | null = (this as any).rootEl || null
      if (fo && this.shouldUpdate()) this.setHtml(fo)
    }
  }

  return { Model, View }
}

function escapeHtml(s: string): string {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// Material Design Icons (mdi:*) used by backend. Render inline SVG with the color.
const ICON_PATHS: Record<string, string> = {
  start: 'M8,5.14V19.14L19,12.14L8,5.14Z',
  end: 'M6,6H18V18H6V6Z',
  llm: 'M12,2A2,2 0 0,1 14,4C14,4.74 13.6,5.39 13,5.73V7H14A7,7 0 0,1 21,14H22A1,1 0 0,1 23,15V18A1,1 0 0,1 22,19H21V20A2,2 0 0,1 19,22H5A2,2 0 0,1 3,20V19H2A1,1 0 0,1 1,18V15A1,1 0 0,1 2,14H3A7,7 0 0,1 10,7H11V5.73C10.4,5.39 10,4.74 10,4A2,2 0 0,1 12,2M9,12A1,1 0 0,0 8,13A1,1 0 0,0 9,14A1,1 0 0,0 10,13A1,1 0 0,0 9,12M15,12A1,1 0 0,0 14,13A1,1 0 0,0 15,14A1,1 0 0,0 16,13A1,1 0 0,0 15,12M8,17C8,17 9,19 12,19C15,19 16,17 16,17H8Z',
  if_else: 'M14,14H22V22H14V14M2,2H10V10H2V2M2,14H10V22H2V14M14,2H22V10H14V2M10.5,11.5L13.5,14.5L10.5,17.5L9,16L11,14L9,12L10.5,11.5Z',
  knowledge_search: 'M12,3C7.58,3 4,6.58 4,11C4,13.13 4.95,15.05 6.46,16.42L5.5,19.5L9,17.5C9.95,17.79 10.96,18 12,18C16.42,18 20,14.42 20,10C20,5.58 16.42,3 12,3M11,7H13V9H11V7M11,10H13V14H11V10Z',
  prompt: 'M4,4H20A2,2 0 0,1 22,6V18A2,2 0 0,1 20,20H4A2,2 0 0,1 2,18V6A2,2 0 0,1 4,4M4,6V18H11V6H4M13,6V18H20V6H13M7,8H9V10H7V8M7,11H9V13H7V11M14,8H18V10H14V8M14,11H18V13H14V11Z',
  http: 'M16,18V21H4A2,2 0 0,1 2,19V8A2,2 0 0,1 4,6H6V4A2,2 0 0,1 8,2H20A2,2 0 0,1 22,4V15A2,2 0 0,1 20,17H16V18M8,4V11L11,8L8,5V4M16,11L13,8L16,5V4H18L15,7L18,10V11H16Z',
  set_var: 'M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M7,9H9V11H7V9M11,9H13V11H11V9M15,9H17V11H15V9Z',
  ifelse: 'M14,14H22V22H14V14M2,2H10V10H2V2M2,14H10V22H2V14M14,2H22V10H14V2M10.5,11.5L13.5,14.5L10.5,17.5L9,16L11,14L9,12L10.5,11.5Z'
}
function iconFor(type: string, color: string): string {
  const path = ICON_PATHS[type] || ICON_PATHS.start
  return `<svg viewBox="0 0 24 24" width="18" height="18" style="fill:${color};"><path d="${path}"/></svg>`
}
