using Confluent.Kafka;
using dotnet_consumer_service.Configurations;
using Microsoft.Extensions.Options;
using Newtonsoft.Json.Linq;
using System.Numerics;

namespace dotnet_consumer_service.Consumer
{
    public class KafkaConsumer : IHostedService, IDisposable
    {

        private readonly ILogger<KafkaConsumer> _logger;

        // Kafka configuration for integration
        private readonly KafkaConfiguration _kafkaConfiguration;
        //The consumer
        private IConsumer<string, string> _consumer;

        public KafkaConsumer(ILogger<KafkaConsumer> logger, IOptions<KafkaConfiguration> kafkaConfigurationOptions)
        {
            _logger = logger ?? throw new ArgumentException(nameof(logger));
            _kafkaConfiguration = kafkaConfigurationOptions?.Value ?? throw new ArgumentException(nameof(kafkaConfigurationOptions));

            // Initialization
            Init();
        }

        private void Init()
        {

            var config = new ConsumerConfig()
            {
                BootstrapServers = _kafkaConfiguration.Brokers,

                GroupId = _kafkaConfiguration.ConsumerGroup,
                SecurityProtocol = SecurityProtocol.Plaintext,
                EnableAutoCommit = true,
                StatisticsIntervalMs = 5000,
                SessionTimeoutMs = 6000,
                AutoOffsetReset = AutoOffsetReset.Earliest,
                EnablePartitionEof = true
            };

            _consumer = new ConsumerBuilder<string, string>(config)
                .SetErrorHandler((_, e) => _logger.LogError($"Error: {e.Reason}"))
                .Build();
        }

        public void Dispose()
        {
            //Dispose consumer
            _consumer.Dispose();
        }

        public async Task StartAsync(CancellationToken cancellationToken)
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                try
                {
                    _logger.LogInformation("Kafka Consumer started.");

                    //Subscribe to topic
                    _consumer.Subscribe(new List<string>() { _kafkaConfiguration.Topic });
                    
                    await Consume(cancellationToken).ConfigureAwait(false);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, ex.Message);
                }
            }
        }

        public async Task StopAsync(CancellationToken cancellationToken)
        {
            _logger.LogInformation("Kafka Consumer is closing consumers.");

            _consumer.Close();

            await Task.CompletedTask;
        }

        private async Task Consume(CancellationToken cancellationToken)
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                try
                {
                    //Consume messages
                    var consumeResult = _consumer.Consume(cancellationToken);

                    if (consumeResult?.Message == null) continue;

                    _logger.LogInformation($"Received Message from topic: [{consumeResult.Message.Value}]");

                    BigInteger orderId = JObject.Parse(consumeResult.Message.Value)["id"].ToObject<BigInteger>();

                    string calculatedBy = "Marcos.Rivas";

                    var price = new decimal(new Random().NextDouble());

                    string requestToPriceCalculator = "{" +
                        "\"orderId\": " + orderId + "," +
                        "\"calculatedBy\": \"" + calculatedBy + "\"," +
                        "\"price\": " + price +
                        "}";

                    HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Post,
                        "http://price-service:8080/entity/prices")
                    {
                        Content = new StringContent(requestToPriceCalculator, System.Text.Encoding.UTF8, "application/json")
                    };

                    HttpClient httpClient = new HttpClient();

                    httpClient.SendAsync(request);

                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, ex.Message);
                }
            }
        }
    }
}