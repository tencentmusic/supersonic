import type { Disposable, IModelService } from '@antv/xflow';
import { createModelServiceConfig, DisposableCollection } from '@antv/xflow';

export namespace NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE {
  export const ID = 'NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE';
  // export const id = 'NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE';
  export interface IState {
    open: boolean;
  }
}

export const useModelServiceConfig = createModelServiceConfig((config) => {
  config.registerModel((registry) => {
    const list: Disposable[] = [
      registry.registerModel({
        id: NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE.ID,
        // getInitialValue: () => {
        //   open: false;
        // },
      }),
    ];
    const toDispose = new DisposableCollection();
    toDispose.pushAll(list);
    return toDispose;
  });
});

export const useOpenState = async (contextService: IModelService) => {
  const ctx = await contextService.awaitModel<NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE.IState>(
    NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE.ID,
  );
  return ctx.getValidValue();
};
