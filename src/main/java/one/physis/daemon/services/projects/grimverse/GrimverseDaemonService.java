package one.physis.daemon.services.projects.grimverse;


import one.physis.daemon.data.repositories.MintRepository;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.data.repositories.ProjectRepository;
import one.physis.daemon.services.DaemonService;
import one.physis.daemon.services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
public class GrimverseDaemonService extends DaemonService<GrimverseWalletService> {

   private final static String project = "grimverse";

   public GrimverseDaemonService(@Value("${projects." + project + ".name}") String name,
                               @Value("${projects." + project + ".id}") int projectId,
                               @Value("${projects." + project + ".customerAddress}") String customerAddress,
                               @Value("${projects." + project + ".percentage}") int percentage,
                               @Value("${projects." + project + ".enabled}") boolean enabled,
                               MintRepository mintRepository,
                               ProjectRepository projectRepository,
                               GrimverseWalletService walletService, //BEWARE OF THIS!
                               NftRepository nftRepository,
                               RetryTemplate retryTemplate,
                               MailService mailService) {
      super(name, projectId, mintRepository, projectRepository, walletService, nftRepository, retryTemplate, mailService, customerAddress, percentage, enabled);
   }

   @Override
   protected Logger getLogger() {
      return LoggerFactory.getLogger(project);
   }
}
