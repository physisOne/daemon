package one.physis.daemon.services.projects.demo;


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
public class DemoDaemonService extends DaemonService<DemoWalletService> {

   private final static String project = "demo";

   public DemoDaemonService(@Value("${projects." + project + ".name}") String name,
                            @Value("${projects." + project + ".id}") int projectId,
                            @Value("${projects." + project + ".customerAddress}") String customerAddress,
                            @Value("${projects." + project + ".percentage}") int percentage,
                            MintRepository mintRepository,
                            ProjectRepository projectRepository,
                            DemoWalletService walletService, //BEWARE OF THIS!
                            NftRepository nftRepository,
                            RetryTemplate retryTemplate,
                            MailService mailService) {
      super(name, projectId, mintRepository, projectRepository, walletService, nftRepository, retryTemplate, mailService, customerAddress, percentage);
   }

   @Override
   protected Logger getLogger() {
      return LoggerFactory.getLogger(project);
   }
}
