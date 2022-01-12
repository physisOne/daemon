package one.physis.daemon.services.dto;

import java.util.ArrayList;
import java.util.List;

public class SendTransaction {
   private List<Output> outputs = new ArrayList<>();

   public List<Output> getOutputs() {
      return outputs;
   }

   public void setOutputs(List<Output> outputs) {
      this.outputs = outputs;
   }

   private List<Input> inputs;

   public List<Input> getInputs() {
      return inputs;
   }

   public void setInputs(List<Input> inputs) {
      this.inputs = inputs;
   }
}
