import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuth } from '../auth/useAuth'
import { getAccessToken, getTokenSubject } from '../auth/tokenStorage'
import { RealtimeContext } from './RealtimeContext'

export const REALTIME_EVENT = 'ymall:realtime'

export function RealtimeProvider({ children }: { children: ReactNode }) {
    const { isAuthenticated, role } = useAuth()
    const clientRef = useRef<Client | null>(null)
    const [connected, setConnected] = useState(false)
    const [connectionVersion, setConnectionVersion] = useState(0)

    useEffect(() => {
        const token = getAccessToken()
        const memberId = getTokenSubject(token)
        if (!isAuthenticated || !token || !memberId) {
            return
        }

        const client = new Client({
            brokerURL: websocketUrl(),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5_000,
            heartbeatIncoming: 10_000,
            heartbeatOutgoing: 10_000,
            onConnect: () => {
                setConnected(true)
                setConnectionVersion((version) => version + 1)
                const handleRealtime = (message: IMessage) => {
                    window.dispatchEvent(new CustomEvent(REALTIME_EVENT, {
                        detail: JSON.parse(message.body) as unknown,
                    }))
                }
                client.subscribe(`/topic/realtime/members/${memberId}`, handleRealtime)
                if (role === 'ROLE_ADMIN') {
                    client.subscribe('/topic/realtime/admin', handleRealtime)
                }
            },
            onDisconnect: () => setConnected(false),
            onWebSocketClose: () => setConnected(false),
            onStompError: () => setConnected(false),
        })
        clientRef.current = client
        client.activate()
        return () => {
            clientRef.current = null
            setConnected(false)
            void client.deactivate()
        }
    }, [isAuthenticated, role])

    const publish = useCallback((destination: string, body: unknown) => {
        const client = clientRef.current
        if (!client?.connected) return false
        client.publish({ destination, body: JSON.stringify(body) })
        return true
    }, [])

    const subscribe = useCallback((destination: string, callback: (body: string) => void) => {
        const client = clientRef.current
        if (!client?.connected) return () => undefined
        const subscription: StompSubscription = client.subscribe(
            destination,
            (message) => callback(message.body),
        )
        return () => subscription.unsubscribe()
    }, [])

    const value = useMemo(() => ({
        connected,
        connectionVersion,
        publish,
        subscribe,
    }), [connected, connectionVersion, publish, subscribe])

    return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>
}

function websocketUrl() {
    if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws`
}
