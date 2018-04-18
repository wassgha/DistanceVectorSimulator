# RIP Simulator

**Lafayette College CS305 Spring 2018.**

A simple network simulation of the distance-vector routing algorithm in Java used by RIP.

## Building the simulation

Build the project using the included Makefile as follows :

```bash
$ make compile
```

## Running the simulation

To run the simulation, initialize multiple routers using the command

```bash
$ java Router [-reverse] configFile
```

then interact with the routers through the command line. Routers will also automatically
send periodical updates to their neighbors.

## Authors

* [Wassim Gharbi](https://github.com/wassgha)
* [Zura Mestiashvili](https://github.com/prosperi)
* [Erik Laucks](https://github.com/laucksy)

## License

Non-commercial, educational use only. Except with written authorization.
