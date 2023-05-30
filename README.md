![Citeck ECOS Logo](https://raw.githubusercontent.com/Citeck/ecos-ui/develop/public/img/logo/ecos-logo.png)

# `ecos-process`

Welcome to the Citeck `ecos-process` repository! This repository contains the process definitions and workflows for the Citeck ECOS platform. Citeck ECOS is a powerful and comprehensive enterprise content and operations system designed to streamline and automate business processes within organizations.

## Get started

If you are new to ECOS platform and would like to load the software locally, we recommend you download the Dockerized version from [Demo repository](https://github.com/Citeck/ecos-community-demo).

## Useful Links

- [Documentation](https://citeck-ecos.readthedocs.io/ru/latest/index.html) provides more in-depth information.

## Dependencies

This application requires the following applications from ECOS deployment to work:

* zookeeper
* rabbitmq
* ecos-model
* ecos-registry

## Development

To start your application in the dev profile, simply run:

```
./mvnw spring-boot:run
```

If your IDE supports starting Spring Boot applications directly, then you can easily run the class 'ru.citeck.ecos.process.EprocApp' without additional setup.

### Building for production

To build the application for production, run:

```
./mvnw -Pprod clean package jib:dockerBuild -Djib.docker.image.tag=custom 
```

To ensure everything worked, stop original ecos-process container and start ecos-process:custom instead of it.

### Testing

To launch your application's tests, run:

```
./mvnw clean test
```

#### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker compose -f docker/sonar.yml up -d
```

Then, run a Sonar analysis:

```
./mvnw -Pprod clean test sonar:sonar
```

## Contributing

We welcome contributions from the community to make ECOS even better. Everyone interacting in the Citeck project’s codebases, issue trackers, chat rooms, and forum is expected to follow the [contributor code of conduct](https://github.com/rubygems/rubygems/blob/master/CODE_OF_CONDUCT.md).

## Support

If you need any assistance or have any questions regarding Citeck `ecos-process`, please create an issue in this repository or reach out to our [support team](mailto:support@citeck.ru).

## License

Citeck `ecos-process` is released under the [GNU Lesser General Public License](LICENSE).
