
export interface FlowNode {
    id: string;
    elemType: 'node' | 'edge';
    position?: { x: number, y: number },
    label?: string;
    originalPosition?: { x: number, y: number };
    description?: string;
    keyPoints?: string[];
    sourceId?: string;
    targetId?: string;
    animated?: boolean;
    color?: string;
    class: string;
}
