import { lazy, type ComponentType } from 'react'

export function lazyNamed<Props, Module>(
    loader: () => Promise<Module>,
    select: (module: Module) => ComponentType<Props>,
) {
    return lazy(async () => ({ default: select(await loader()) }))
}
