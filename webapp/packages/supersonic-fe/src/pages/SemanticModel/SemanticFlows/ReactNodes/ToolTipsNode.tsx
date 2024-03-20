import React from 'react';
import ReactDom from 'react-dom';
import { Tooltip } from 'antd';
import type { EdgeView } from '@antv/x6';
import { Graph, ToolsView } from '@antv/x6';
class TooltipTool extends ToolsView.ToolItem<EdgeView, TooltipToolOptions> {
  private knob: HTMLDivElement;

  render() {
    if (!this.knob) {
      this.knob = ToolsView.createElement('div', false) as HTMLDivElement;
      this.knob.style.position = 'absolute';
      this.container.appendChild(this.knob);
    }
    return this;
  }

  private toggleTooltip(visible: boolean) {
    if (this.knob) {
      ReactDom.unmountComponentAtNode(this.knob);
      if (visible) {
        ReactDom.render(
          <Tooltip title={this.options.tooltip} open={visible} destroyTooltipOnHide>
            <div />
          </Tooltip>,
          this.knob,
        );
      }
    }
  }

  private onMosueEnter({ e }: { e: MouseEvent }) {
    this.updatePosition(e);
    this.toggleTooltip(true);
  }

  private onMouseLeave() {
    this.updatePosition();
    this.toggleTooltip(false);
  }

  private onMouseMove() {
    this.updatePosition();
    this.toggleTooltip(false);
  }

  delegateEvents() {
    this.cellView.on('cell:mouseenter', this.onMosueEnter, this);
    this.cellView.on('cell:mouseleave', this.onMouseLeave, this);
    this.cellView.on('cell:mousemove', this.onMouseMove, this);
    return super.delegateEvents();
  }

  private updatePosition(e?: MouseEvent) {
    const style = this.knob.style;
    if (e) {
      const p = this.graph.clientToGraph(e.clientX, e.clientY);
      style.display = 'block';
      style.left = `${p.x}px`;
      style.top = `${p.y}px`;
    } else {
      style.display = 'none';
      style.left = '-1000px';
      style.top = '-1000px';
    }
  }

  protected onRemove() {
    this.toggleTooltip(false);
    this.cellView.off('cell:mouseenter', this.onMosueEnter, this);
    this.cellView.off('cell:mouseleave', this.onMouseLeave, this);
    this.cellView.off('cell:mousemove', this.onMouseMove, this);
  }
}

TooltipTool.config({
  tagName: 'div',
  isSVGElement: false,
});

export interface TooltipToolOptions extends ToolsView.ToolItem.Options {
  tooltip?: string;
}

export const registerEdgeTool = () => {
  Graph.registerEdgeTool('tooltip', TooltipTool, true);
};
