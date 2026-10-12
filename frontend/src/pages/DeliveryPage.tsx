import { useParams } from "react-router";
import { DeliveryDetails } from "@/features/deliveries/components";

export function DeliveryPage() {
  const { deliveryId } = useParams<{
    deliveryId: string;
  }>();

  if (!deliveryId) {
    return null;
  }

  return (
    <DeliveryDetails deliveryId={deliveryId} />
  );
}
