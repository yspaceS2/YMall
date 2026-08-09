import { createContext } from 'react'

export interface RealtimeContextValue {
    connected: boolean
    connectionVersion: number
    publish: (destination: string, body: unknown) => boolean
    subscribe: (destination: string, callback: (body: string) => void) => (() => void)
}

export const RealtimeContext = createContext<RealtimeContextValue | null>(null)
