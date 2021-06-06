package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class GroupConfiguration extends ServiceConfigurationBase implements INameable, SerializableObject {

  protected String name;
  protected Collection<String> jvmOptions = new ArrayList<>();
  protected Collection<String> processParameters = new ArrayList<>();

  protected Collection<ServiceEnvironmentType> targetEnvironments = new ArrayList<>();

  public GroupConfiguration() {
  }

  public GroupConfiguration(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments, String name, Collection<String> jvmOptions,
    Collection<ServiceEnvironmentType> targetEnvironments) {
    this(includes, templates, deployments, name, jvmOptions, new ArrayList<>(), targetEnvironments);
  }

  public GroupConfiguration(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments, String name, Collection<String> jvmOptions,
    Collection<String> processParameters, Collection<ServiceEnvironmentType> targetEnvironments) {
    super(includes, templates, deployments);
    this.name = name;
    this.jvmOptions = jvmOptions;
    this.processParameters = processParameters;
    this.targetEnvironments = targetEnvironments;
  }


  public GroupConfiguration(String name) {
    this.name = name;
  }

  public Collection<String> getJvmOptions() {
    return this.jvmOptions;
  }

  @Override
  public Collection<String> getProcessParameters() {
    return this.processParameters;
  }

  public Collection<ServiceEnvironmentType> getTargetEnvironments() {
    return this.targetEnvironments;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    super.write(buffer);
    buffer.writeString(this.name);
    buffer.writeStringCollection(this.jvmOptions);
    buffer.writeStringCollection(this.processParameters);

    buffer.writeVarInt(this.targetEnvironments.size());
    for (ServiceEnvironmentType environment : this.targetEnvironments) {
      buffer.writeEnumConstant(environment);
    }
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    super.read(buffer);
    this.name = buffer.readString();
    this.jvmOptions = buffer.readStringCollection();
    this.processParameters = buffer.readStringCollection();

    int size = buffer.readVarInt();
    this.targetEnvironments = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      this.targetEnvironments.add(buffer.readEnumConstant(ServiceEnvironmentType.class));
    }
  }
}
