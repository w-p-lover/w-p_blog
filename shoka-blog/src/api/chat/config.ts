import {Client} from "@stomp/stompjs";
import {ChatMessage} from "@/api/chat/type";
import SockJS from "sockjs-client";
import useStore from "@/store";
import {UnwrapRef} from "vue";
import {UserState} from "@/store/types";
const {user} = useStore();

export default class WebSocketService {
    private stompClient: Client | null = null;
    private userId: UnwrapRef<UserState["id"]> | undefined;
    private statusCallback?: (connected: boolean) => void;

    constructor() {
        this.userId = user.id
    }

    isConnected(): boolean {
        return Boolean(this.stompClient?.connected);
    }
    /**
     * 初始化 WebSocket 连接
     * @param onMessageCallback 消息接收回调函数
     */
    connect(onMessageCallback: (message: ChatMessage) => void, onStatusChange?: (connected: boolean) => void): void {
        this.statusCallback = onStatusChange;
        const socket = new SockJS('http://localhost:8080/chat');
        this.stompClient = new Client({
            webSocketFactory: () => socket as WebSocket,
            reconnectDelay: 5000,
        });

        // 订阅服务端消息
        this.stompClient.onConnect = () => {
            this.statusCallback?.(true);
            if (this.stompClient && this.userId) {
                this.stompClient.subscribe("/queue/messages/" + this.userId, (message) => {
                    const parsedMessage: ChatMessage = JSON.parse(message.body);
                    onMessageCallback(parsedMessage);
                });
            }
        };

        this.stompClient.onDisconnect = () => {
            this.statusCallback?.(false);
        };

        this.stompClient.onWebSocketClose = () => {
            this.statusCallback?.(false);
        };

        // 错误处理
        this.stompClient.onStompError = (frame) => {
            this.statusCallback?.(false);
            console.error('Broker reported error:', frame.headers['message']);
            console.error('Additional details:', frame.body);
        };

        this.stompClient.activate();
    }

    /**
     * 发送消息
     * @param message 消息内容
     */
    sendMessage(message: ChatMessage): boolean {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: '/app/send',
                body: JSON.stringify(message),
            });
            return true;
        } else {
            console.error('WebSocket is not connected');
            return false;
        }
    }

    /**
     * 断开 WebSocket 连接
     */
    disconnect(): void {
        if (this.stompClient) {
            this.stompClient.deactivate().then(() => {
                this.statusCallback?.(false);
            }).catch((error) => {
                console.error('Error while disconnecting:', error);
            });
        }
    }
}
