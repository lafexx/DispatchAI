import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { createCustomer, createDelivery } from "@/api";
import { useCustomers, refreshOperations } from "@/lib/queries";
import { Modal, Notice } from "@/components/UI";
export function CreateDeliveryModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const customers = useCustomers();
  const client = useQueryClient();
  const navigate = useNavigate();
  const [newCustomer, setNewCustomer] = useState(false);
  const [createdCustomerId, setCreatedCustomerId] = useState<string | null>(
    null,
  );
  const mutation = useMutation({
    mutationFn: async (form: FormData) => {
      let customerId = String(form.get("customerId") ?? "");
      if (newCustomer) {
        customerId =
          createdCustomerId ??
          (
            await createCustomer({
              name: String(form.get("name")).trim(),
              email: String(form.get("email")).trim(),
            })
          ).id;
        setCreatedCustomerId(customerId);
        void client.invalidateQueries({ queryKey: ["customer"] });
      }
      return createDelivery({
        customerId,
        pickupAddress: String(form.get("pickup")).trim(),
        destinationAddress: String(form.get("destination")).trim(),
        parcelWeightKg: Number(form.get("weight")),
      });
    },
    onSuccess: async (delivery) => {
      await refreshOperations(client);
      onClose();
      navigate(`/deliveries/${delivery.id}`);
    },
  });
  return (
    <Modal
      title="Create a delivery"
      description="Add the customer, route and parcel weight. Choose resources after creating the delivery."
      open={open}
      onClose={onClose}
      busy={mutation.isPending}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!mutation.isPending)
            mutation.mutate(new FormData(e.currentTarget));
        }}
        className="stack"
      >
        <div className="flex gap-2">
          <button
            className={`button button-small ${!newCustomer ? "button-primary" : ""}`}
            type="button"
            disabled={mutation.isPending || !!createdCustomerId}
            onClick={() => setNewCustomer(false)}
          >
            Existing customer
          </button>
          <button
            className={`button button-small ${newCustomer ? "button-primary" : ""}`}
            type="button"
            disabled={mutation.isPending}
            onClick={() => setNewCustomer(true)}
          >
            New customer
          </button>
        </div>
        {newCustomer ? (
          <div className="form-grid">
            <label className="field">
              Customer name
              <input
                name="name"
                required
                maxLength={150}
                disabled={!!createdCustomerId || mutation.isPending}
              />
            </label>
            <label className="field">
              Email
              <input
                name="email"
                type="email"
                required
                disabled={!!createdCustomerId || mutation.isPending}
              />
            </label>
            {createdCustomerId && (
              <p className="full muted text-xs">
                Customer saved. Complete the delivery details below.
              </p>
            )}
          </div>
        ) : (
          <label className="field">
            Customer
            <select
              name="customerId"
              required
              disabled={customers.isPending || mutation.isPending}
            >
              <option value="">Select a customer</option>
              {customers.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} · {c.email}
                </option>
              ))}
            </select>
            {customers.isError && (
              <Notice tone="error">
                Customers could not be loaded. Retry or add a new customer.
              </Notice>
            )}
          </label>
        )}
        <label className="field">
          Pickup address
          <input
            name="pickup"
            required
            maxLength={500}
            placeholder="e.g. 12 King Street, Sydney"
            disabled={mutation.isPending}
          />
        </label>
        <label className="field">
          Destination address
          <input
            name="destination"
            required
            maxLength={500}
            placeholder="e.g. 45 George Street, Parramatta"
            disabled={mutation.isPending}
          />
        </label>
        <label className="field">
          Parcel weight (kg)
          <input
            name="weight"
            type="number"
            min="0.01"
            max="99999999.99"
            step="0.01"
            required
            placeholder="e.g. 25.00"
            disabled={mutation.isPending}
          />
          <small>
            Dispatch will show available vehicles that can carry this weight.
          </small>
        </label>
        {mutation.isError && (
          <Notice tone="error">{mutation.error.message}</Notice>
        )}
        <div className="form-actions">
          <button
            className="button"
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
          >
            Cancel
          </button>
          <button
            className="button button-primary"
            disabled={
              mutation.isPending || (!newCustomer && customers.isPending)
            }
            type="submit"
          >
            {mutation.isPending ? "Creating…" : "Create delivery"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
