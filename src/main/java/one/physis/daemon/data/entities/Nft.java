package one.physis.daemon.data.entities;

import javax.persistence.*;

@Entity
@Table(name = "mai_pad_nfts")
public class Nft {

   @Id
   private Integer id;

   private Integer number;

   private String ipfs;

   @Column(unique = true)
   private String token;

   private boolean taken;

   private boolean burned;

   private String burnTransaction;

   @Lob
   private String attributes;

   @ManyToOne
   private Mint mint;

   @ManyToOne
   private Project project;

   private String filename;

   public Integer getId() {
      return id;
   }

   public void setId(Integer id) {
      this.id = id;
   }

   public Integer getNumber() {
      return number;
   }

   public void setNumber(Integer number) {
      this.number = number;
   }

   public boolean isTaken() {
      return taken;
   }

   public void setTaken(boolean taken) {
      this.taken = taken;
   }

   public String getToken() {
      return token;
   }

   public void setToken(String token) {
      this.token = token;
   }

   public Mint getMint() {
      return mint;
   }

   public void setMint(Mint mint) {
      this.mint = mint;
   }

   public String getIpfs() {
      return ipfs;
   }

   public void setIpfs(String ipfs) {
      this.ipfs = ipfs;
   }

   public Project getProject() {
      return project;
   }

   public void setProject(Project project) {
      this.project = project;
   }

   public String getAttributes() {
      return attributes;
   }

   public void setAttributes(String attributes) {
      this.attributes = attributes;
   }

   public String getFilename() {
      return filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public boolean isBurned() {
      return burned;
   }

   public void setBurned(boolean burned) {
      this.burned = burned;
   }

   public String getBurnTransaction() {
      return burnTransaction;
   }

   public void setBurnTransaction(String burnTransaction) {
      this.burnTransaction = burnTransaction;
   }
}
