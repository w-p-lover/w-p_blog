
export interface FlowNode {
    id: string;
    elemType: 'input' | 'default' | 'output';
    positionX: number;
    positionY: number;
    label: string;
    description?: string;
    keyPoints?: string[];
    sourceId: string;
    targetId: string;
    animated?: boolean;
    color?: string;
    elemClass?: string;
}
