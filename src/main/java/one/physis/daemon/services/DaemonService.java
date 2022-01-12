package one.physis.daemon.services;

import one.physis.daemon.data.entities.Mint;
import one.physis.daemon.data.entities.Nft;
import one.physis.daemon.data.entities.Project;
import one.physis.daemon.data.entities.enums.MintState;
import one.physis.daemon.data.repositories.MintRepository;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.data.repositories.ProjectRepository;
import org.slf4j.Logger;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class DaemonService<T extends WalletService> {

   private final MintRepository mintRepository;
   private final T walletService;
   private final NftRepository nftRepository;
   private final RetryTemplate retryTemplate;
   private final MailService mailService;
   private final String name;
   private final int price;
   private final int htrPrice;
   private final Project project;

   protected abstract Logger getLogger();

   public DaemonService(String name,
                        int projectId,
                        MintRepository mintRepository,
                        ProjectRepository projectRepository,
                        T walletService,
                        NftRepository nftRepository,
                        RetryTemplate retryTemplate,
                        MailService mailService){
      this.name = name;
      this.mintRepository = mintRepository;
      this.walletService = walletService;
      this.nftRepository = nftRepository;
      this.retryTemplate = retryTemplate;
      this.mailService = mailService;
      this.project = projectRepository.findById(projectId).get();
      this.price = this.project.getPrice();
      this.htrPrice = this.project.getPrice() * 100;
   }

   //@Scheduled(fixedDelay = 10000)
   public void checkAddresses() {
      getLogger().info("Loop started");

      //LOADING MINTS TO PROCESS
      final List<Mint> mints = new ArrayList<>();
      try {
         retryTemplate.execute(context -> {
            mints.addAll(this.mintRepository.getAllByStateAndProject(MintState.WAITING_FOR_DEPOSIT.ordinal(), this.project));
            return null;
         });
         retryTemplate.execute(context -> {
            mints.addAll(this.mintRepository.getAllByStateAndProject(MintState.SENDING_NFT.ordinal(), this.project));
            return null;
         });
      } catch (Exception ex) {
         getLogger().error("Failed to get mints from database", ex);
      }
      //END OF LOADING MINTS TO PROCESS

      //LOADING MINTS TO SEND DEPOSIT BACK
      final List<Mint> returnDepositMints = new ArrayList<>();
      try {
         retryTemplate.execute(context -> {
            returnDepositMints.addAll(this.mintRepository.getAllByStateAndProject(MintState.OUT_OF_NFT.ordinal(), this.project));
            return null;
         });
      } catch(Exception ex) {
         getLogger().error("Failed to get mints for OUT OF NFT from database", ex);
      }
      for(Mint mint : returnDepositMints) {
         sendDepositBack(mint);
      }
      //END OF LOADING MINTS TO SEND DEPOSIT BACK

      List<Mint> notDeadMints = mints.stream().filter(mint -> !mint.isDead()).collect(Collectors.toList());
      getLogger().info("Processing mints " + notDeadMints.size());

      Integer receiveBalance = walletService.checkHtrBalance(false);
      if(receiveBalance != null) {
         getLogger().info("Receive balance is " + (receiveBalance / 100) + " HTR");
         if(receiveBalance / 100 > 500000) {
            getLogger().warn("WE SHOULD STOP SELLING!");
         }
      }

      for (Mint mint : notDeadMints) {
         walletService.checkWallets();

         receiveBalance = walletService.checkHtrBalance(false);
         if(receiveBalance != null) {
            getLogger().info("Receive balance is " + (receiveBalance / 100) + " HTR");
            if(receiveBalance / 100 > 500000) {
               getLogger().warn("WE SHOULD STOP SELLING!");
            }
         }

         try {
            getLogger().info("========================================================================");
            getLogger().info("Processing mint " + mint.getId());
            if(mint.getState() == MintState.WAITING_FOR_DEPOSIT.ordinal()) {
               getLogger().info("Mint is in WAITING_FOR_DEPOSIT state");
               Integer balance = walletService.checkBalance(mint.getDepositAddress().getAddress());
               getLogger().info("Balance is " + balance + " and it should be " + (this.htrPrice * mint.getCount()));
               if(balance != null && balance > 0) {
                  mint.setBalance(balance);
                  retryTemplate.execute(context -> {
                     mintRepository.save(mint);
                     return null;
                  });
               }
               if (balance != null && balance >= this.htrPrice * mint.getCount()) {
                  getLogger().info("Balance is good, initializing nfts");
                  if(initNfts(mint)) {
                     getLogger().info("Setting mint " + mint.getId() + " state to SENDING_NFT");
                     mint.setState(MintState.SENDING_NFT.ordinal());
                     retryTemplate.execute(context -> {
                        mintRepository.save(mint);
                        return null;
                     });
                     send(mint);
                  }
               }
               if (balance != null && balance == 0) {
                  Date created = mint.getCreatedAt();
                  Date now = new Date();

                  long diff = now.getTime() - created.getTime();
                  long hours = TimeUnit.MILLISECONDS.toHours(diff);
                  if(hours >= 3) {
                     getLogger().info("Setting mint " + mint.getId() + " as dead");
                     mint.setDead(true);
                     mintRepository.save(mint);
                  }
               }
            }
            else {
               send(mint);
            }
            Thread.sleep(3000);
         } catch (Exception ex) {
            getLogger().error("Mint failed " + mint.getId(), ex);
         }
         getLogger().info("-----------------------------------------------------------------------------");
      }
      getLogger().info("Loop ended");
   }

   private boolean initNfts(Mint mint) {
      try {
         if (mint.getNfts() == null || mint.getNfts().isEmpty()) {
            List<Nft> nfts = new ArrayList<>();
            getLogger().info("Finding nfts");
            try {
               retryTemplate.execute(context -> {
                  nfts.addAll(nftRepository.findNotTaken(mint.getCount(), mint.getProject().getId()));
                  return null;
               });
            } catch (Exception ex) {
               getLogger().error("Failed to initialize nfts for mint " + mint.getId());
               return false;
            }

            if(nfts == null || nfts.size() < mint.getCount()) {
               getLogger().warn("We are out of nfts " + mint.getId());
               mint.setState(MintState.OUT_OF_NFT.ordinal());
               retryTemplate.execute(context -> {
                  mintRepository.save(mint);
                  return null;
               });
               return false;
            }

            for (Nft s : nfts) {
               getLogger().info("Setting nft " + s.getId() + " as taken!");
               s.setTaken(true);
               s.setMint(mint);
            }

            getLogger().info("Saving nfts");
            retryTemplate.execute(context -> {
               nftRepository.saveAll(nfts);
               return null;
            });

            mint.setNfts(new HashSet<>(nfts));
            getLogger().info("Saving mint " + mint.getId());
            retryTemplate.execute(context -> {
               mintRepository.save(mint);
               return null;
            });
         }
      } catch (Exception ex) {
         getLogger().error("Failed to init nfts for mint " + mint.getId(), ex);
         return false;
      }
      return true;
   }

   private void send(Mint mint) throws Exception {
      getLogger().info("Sending NFT for mint " + mint.getId());
      List<String> tokens = new ArrayList<>();
      for(Nft nft : mint.getNfts()) {
         tokens.add(nft.getToken());
      }
      String transactionHash = walletService.sendTokens(mint.getUserAddress(), tokens);
      if(transactionHash != null) {
         getLogger().info("Transaction hash " + transactionHash);
         mint.setTransaction(transactionHash);
         mint.setTransactionDate(new Date());
         mint.setState(MintState.NFT_SENT.ordinal());
         getLogger().info("Setting mint " + mint.getId() + " state to NFT_SENT");
         try {
            retryTemplate.execute(context -> {
               mintRepository.save(mint);
               return null;
            });
         } catch (Exception ex) {
            getLogger().error("FATAL! Could not save mint " + mint.getId() + " to state NFT_SENT when NFTS were sent!", ex);
         }
         if(mint.getEmail() != null) {
            mailService.sendMail(mint);
         }
      }
   }

   private void sendDepositBack(Mint mint) {
      if(mint.getBalance() != null && mint.getBalance() > 0) {
         getLogger().info("Sending deposit back for mint " + mint.getId());
         String transactionHash = walletService.sendHtrFromReceive(mint.getUserAddress(), mint.getBalance());
         if (transactionHash != null) {
            getLogger().info("Transaction hash " + transactionHash);
            mint.setSendBackTransaction(transactionHash);
            mint.setState(MintState.HTR_SENT_BACK.ordinal());
            getLogger().info("Setting mint " + mint.getId() + " state to HTR_SENT_BACK");
            try {
               retryTemplate.execute(context -> {
                  mintRepository.save(mint);
                  return null;
               });
            } catch (Exception ex) {
               getLogger().error("FATAL! Could not save mint " + mint.getId() + " to state HTR_SENT_BACK when HTR was sent back!", ex);
            }
         }
      }
   }
}
