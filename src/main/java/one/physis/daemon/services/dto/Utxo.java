package one.physis.daemon.services.dto;

public class Utxo {
   private String address;
   private int amount;
   private String txId;
   private boolean locked;
   private int index;

   public String getAddress() {
      return address;
   }

   public void setAddress(String address) {
      this.address = address;
   }

   public int getAmount() {
      return amount;
   }

   public void setAmount(int amount) {
      this.amount = amount;
   }

   public String getTxId() {
      return txId;
   }

   public void setTxId(String txId) {
      this.txId = txId;
   }

   public boolean isLocked() {
      return locked;
   }

   public void setLocked(boolean locked) {
      this.locked = locked;
   }

   public int getIndex() {
      return index;
   }

   public void setIndex(int index) {
      this.index = index;
   }
}
