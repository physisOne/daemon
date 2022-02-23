package one.physis.daemon.services;

import com.google.gson.Gson;
import one.physis.daemon.data.entities.Mint;
import one.physis.daemon.data.entities.Nft;
import one.physis.daemon.data.repositories.MintRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class MailService {

   //MAIL
   private static String MINT_URL = "{MINT_URL}";
   private static String TILES = "{TILES}";
   //TILE
   private static String NAME = "{NAME}";
   private static String IMAGE_SRC = "{IMAGE_SRC}";
   private static String ATTRIBUTES = "{ATTRIBUTES}";
   private static String TOKEN = "{TOKEN}";
   //ATTRIBUTES
   private static String VALUE = "{VALUE}";
   private static String RARITY = "{RARITY}";

   private Session session;
   private String from = "physis@physis.one";
   private String host = "smtp.m1.websupport.sk";
   @Value("${mail.password}")
   private String password;

   Logger logger = LoggerFactory.getLogger(MailService.class);

   private final MintRepository mintRepository;
   private final Gson gson;

   public MailService(MintRepository mintRepository) {
      this.mintRepository = mintRepository;
      this.gson = new Gson();
   }

   @PostConstruct
   public void init() {
      Properties properties = System.getProperties();

      properties.put("mail.smtp.host", host);
      properties.put("mail.smtp.port", "465");
      properties.put("mail.smtp.ssl.enable", "true");
      properties.put("mail.smtp.auth", "true");

      this.session = Session.getInstance(properties, new javax.mail.Authenticator() {
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(from, password);
         }
      });

//      Mint mint = mintRepository.findById("25df8c87-6fca-4212-8172-07acd0a8dea3").get();
//      sendMail(mint);
   }

   public void sendMail(Mint mint) {
      if (mint.getEmail() != null && isValidEmailAddress(mint.getEmail())) {
         try {
            logger.info("Sending mail to " + mint.getEmail());

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(mint.getEmail()));
            message.setSubject("Your " + mint.getProject().getName() + " NFTs are ready!");
            String html = getHtml(mint);
            message.setContent(html, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Mail sent successfully to " + mint.getEmail());
         } catch (Exception ex) {
            logger.error("Could not send mail to " + mint.getEmail(), ex);
         }
      }
   }

   private boolean isValidEmailAddress(String email) {
      boolean result = true;
      try {
         InternetAddress emailAddr = new InternetAddress(email);
         emailAddr.validate();
      } catch (AddressException ex) {
         result = false;
      }
      return result;
   }

   private String getHtml(Mint mint) throws Exception {
      String mail = getTextFromFile("mail/mail.html");
      String tile = getTextFromFile("mail/tile.html");
      String attribute = getTextFromFile("mail/attribute.html");

      mail = mail.replace(MINT_URL, "https://www.physis.one/" + mint.getProject().getSlug() + "/" + mint.getId() + "/sent");
      mail = mail.replace(NAME, mint.getProject().getName());

      String tiles = "";
      for(Nft nft : mint.getNfts()) {
         String nftTile = tile.replace(NAME, mint.getProject().getName() + " no. " + nft.getNumber());
         String imgUrl = "https://www.physis.one/storage/app/media/" + mint.getProject().getSlug() + "/nfts/" + nft.getFilename();
         nftTile = nftTile.replace(IMAGE_SRC, imgUrl);
         nftTile = nftTile.replace(TOKEN, nft.getToken());

         String attributes = "";
         NftAttributes nftAttributes = gson.fromJson(nft.getAttributes(), NftAttributes.class);
         for(NftAttribute attr : nftAttributes.getAttributes()) {
            String nftAttr = attribute.replace(NAME, attr.getType());
            nftAttr = nftAttr.replace(VALUE, attr.getValue());

            BigDecimal rarity = new BigDecimal(attr.getRarity() / (float) mint.getProject().getNftsCount() * 100);
            rarity = rarity.setScale(2, RoundingMode.HALF_UP);
            nftAttr = nftAttr.replace(RARITY, rarity.toString());
            attributes += nftAttr;
         }
         nftTile = nftTile.replace(ATTRIBUTES, attributes);
         tiles += nftTile;
      }
      mail = mail.replace(TILES, tiles);

      return mail;
   }

   private String getTextFromFile(String fileName) throws Exception {
      InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
      String text = IOUtils.toString(is, StandardCharsets.UTF_8.name());

      return text;
   }

   public class NftAttribute {
      private String type;
      private String value;
      private int rarity;

      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      public String getValue() {
         return value;
      }

      public void setValue(String value) {
         this.value = value;
      }

      public int getRarity() {
         return rarity;
      }

      public void setRarity(int rarity) {
         this.rarity = rarity;
      }
   }

   public class NftAttributes {
      private List<NftAttribute> attributes = new ArrayList<>();

      public List<NftAttribute> getAttributes() {
         return attributes;
      }

      public void setAttributes(List<NftAttribute> attributes) {
         this.attributes = attributes;
      }
   }
}
