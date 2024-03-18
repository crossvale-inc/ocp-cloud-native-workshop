namespace dotnet_consumer_service.Configurations
{
    public class KafkaConfiguration
    {
        public string Brokers { get; set; }
        public string Topic { get; set; }
        public string ConsumerGroup { get; set; }
    }
}