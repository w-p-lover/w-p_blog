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
    constructor(userId: string) {
        this.userId = user.id
    }
    /**
     * 初始化 WebSocket 连接
     * @param onMessageCallback 消息接收回调函数
     */
    connect(onMessageCallback: (message: ChatMessage) => void): void {
        const socket = new SockJS('http://localhost:8080/chat');
        this.stompClient = new Client({
            webSocketFactory: () => socket as WebSocket,
            reconnectDelay: 5000,
        });

        // 订阅服务端消息
        this.stompClient.onConnect = () => {
            console.log('WebSocket connected');
            if (this.stompClient && this.userId) {
                this.stompClient.subscribe("/queue/messages/" + this.userId, (message) => {
                    console.log("websocket1:"+this.userId)
                    const parsedMessage: ChatMessage = JSON.parse(message.body);
                    onMessageCallback(parsedMessage);
                });
            }
        };

        // 错误处理
        this.stompClient.onStompError = (frame) => {
            console.error('Broker reported error:', frame.headers['message']);
            console.error('Additional details:', frame.body);
        };

        this.stompClient.activate();
    }

    /**
     * 发送消息
     * @param message 消息内容
     */
    sendMessage(message: ChatMessage): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: '/app/send',
                body: JSON.stringify(message),
            });
        } else {
            console.error('WebSocket is not connected');
        }
    }

    /**
     * 断开 WebSocket 连接
     */
    disconnect(): void {
        if (this.stompClient) {
            this.stompClient.deactivate().then(() => {
                console.log('WebSocket disconnected');
            }).catch((error) => {
                console.error('Error while disconnecting:', error);
            });
        }
    }
}
