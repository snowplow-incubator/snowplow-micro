import React, { useRef } from 'react';
import { useDrag, useDrop } from 'react-dnd';
import { GripVertical } from 'lucide-react';

type DragItem = {
  type: string;
  index: number;
}

type DraggableColumnProps = {
  index: number; // This is the index in selectedColumns (starts from 0 for first draggable column)
  onReorder: (fromIndex: number, toIndex: number) => void;
  children: React.ReactNode;
  className?: string;
}

export function DraggableColumn({
  index,
  onReorder,
  children,
  className = ''
}: DraggableColumnProps) {
  const ref = useRef<HTMLDivElement>(null);

  const [{ isDragging }, drag] = useDrag({
    type: 'column',
    item: { type: 'column', index },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  });

  const [{ isOver, canDrop }, drop] = useDrop({
    accept: 'column',
    drop: (item: DragItem) => {
      if (!ref.current) {
        return;
      }

      const dragIndex = item.index;
      const hoverIndex = index;

      if (dragIndex === hoverIndex) {
        return;
      }

      // Call the reorder function with indexes relative to selectedColumns
      onReorder(dragIndex, hoverIndex);

      // Update the item index for continued dragging
      item.index = hoverIndex;
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
    }),
  });

  // Connect drag and drop to the DOM element
  drag(drop(ref));

  // Visual styling based on drag/drop state
  const dragStyle = isDragging ? 'opacity-50' : '';
  const dropStyle = isOver && canDrop ? 'bg-blue-50 border-blue-200' : '';

  return (
    <div
      ref={ref}
      className={`cursor-grab active:cursor-grabbing flex items-center ${dragStyle} ${dropStyle} ${className}`.trim()}
    >
      <GripVertical className="h-4 w-4 text-gray-400" />
      {children}
    </div>
  );
}