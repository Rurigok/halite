#!/usr/bin/env python3
from collections import namedtuple
import multiprocessing as mp
import random
import subprocess
import sys

# Represents a single playerID and score pair from a single game
Result = namedtuple('Result', 'playerID score')
cores = mp.cpu_count()
class Simulation(object):
    """Represents a single run of a game.

    The purpose of this class is to take in any necessary parameters for running
    a game and return the ranked results of that game.
    """

    def __init__(self, boardWidth, boardHeight, runCommands, numPlayers):
        self.boardWidth = boardWidth
        self.boardHeight = boardHeight
        self.runCommands = runCommands
        self.numPlayers = numPlayers

    def __call__(self):
        # Execute Halite as a subprocess
        proc = subprocess.Popen(["./halite", "-q", "-d", "{} {}".format(self.boardWidth, self.boardHeight)] + self.runCommands,
                                stdout=subprocess.PIPE)

        # Collect stdout
        lines = []
        for line in proc.stdout:
            lines.append(line.decode("utf-8").rstrip())

        results = []
        # Parse player number / rank from Halite output
        # Example result output from halite executable:
        # 1 3           (player 1 received 3rd place)
        # 2 2           (player 2 received 2nd place)
        # 3 1           (player 3 received 1st place)
        for line in lines[self.numPlayers + 1:]:
            if line:
                playerID, rank = [int(x) for x in line.split()]
                score = 100 - 100 / (self.numPlayers - 1) * (rank - 1)
                # Collect results for each player
                results.append(Result(playerID, score))
        return results

class SimulationWorker(mp.Process):
    """Worker process that consumes and completes Simulations.

    Continually pulls from a simQueue containing Simulation objects and runs
    them until we encounter a None job, processing each result into the
    resultQueue.
    """

    def __init__(self, simQueue, resultQueue, outputLock, players):
        mp.Process.__init__(self)
        self.simQueue = simQueue
        self.resultQueue = resultQueue
        self.outputLock = outputLock
        self.players = players

    def run(self):
        # Pull and run simulations until we encounter a poison pill (None)
        while True:
            nextSim = self.simQueue.get()
            if nextSim is None:
                self.simQueue.task_done()
                break
            results = nextSim()
            self.simQueue.task_done()
            for result in results:
                self.resultQueue.put(result)
            # Acquire a lock so our messages to stdout don't get jumbled and
            # print the rankings of the now-completed simulation
            with self.outputLock:
                print("=========================================")
                i = 1
                for result in sorted(results, key=lambda r: r.score, reverse=True):
                    print("{} got rank #{}".format(self.players[result.playerID].playerName, i))
                    i += 1

class Player(object):

    def __init__(self, playerName):
        self.playerName = playerName
        self.totalScore = 0

    def __str__(self):
        return "{} has a total rank of {}".format(self.playerName, self.totalScore)

def usage(message):
    print(message)
    print("Usage: {} <# of runs> <bot main files...>".format(sys.argv[0]))
    exit()

def main():
    players = dict()
    runCommands = list()
    runs = -1
    playerID = 1

    # Parse # of runs, compile any files and build run command + player list.
    for arg in enumerate(sys.argv[1:]):
        if arg.endswith(".java"):
            subprocess.call(["javac", arg])
            runCommands.append("java " + arg[:-5])
            players[playerID] = Player(arg[:-5])
            playerID += 1
        elif arg.endswith(".py"):
            runCommands.append("python3 " + arg)
            players[playerID] = Player(arg[:-2])
            playerID += 1
        elif arg.isdigit():
            runs = int(arg)
        elif arg == '-d':
            continue
        else:
            runCommands.append("./" + arg)
            players[playerID] = Player(arg)
            playerID += 1

    numPlayers = playerID - 1

    if runs == -1:
        usage("The number of runs must be supplied.")

    print("Compilation finished. Starting simulation(s)...")

    # Generate candidate board sizes (only square boards are generated)
    sizes = [x for x in range(45, 50, 5)]

    # Create input/output queues
    simQueue = mp.JoinableQueue()
    resultQueue = mp.Queue()
    outputLock = mp.Lock()

    # Create and start simulation worker processes
    numWorkers = mp.cpu_count()
    workers = [SimulationWorker(simQueue, resultQueue, outputLock, players)
               for i in range(numWorkers)]
    for w in workers:
        w.start()

    # Enqueue all simulations (games) to be run
    for i in range(runs):
        size = random.choice(sizes)
        simQueue.put(Simulation(size, size, runCommands, numPlayers))

    # Add a None for each worker to signal that they are done,
    # i.e. a "posion pill"
    for i in range(numWorkers):
        simQueue.put(None)

    # Wait for all jobs to finish
    simQueue.join()

    # Process results
    while not resultQueue.empty():
        result = resultQueue.get()
        players[result.playerID].totalScore += result.score

    print("=========================================")
    print("###### Final Scores (0-100 scale) #######")

    for player in players.values():
        score = int(player.totalScore / (100 * runs) * 100)
        print("{}: {}".format(player.playerName, score))

if __name__ == '__main__':
    main()

    if '-d' in sys.argv:
        import os
        files = os.listdir()

        for f in files:
            if f.endswith('.hlt') or f.endswith('.log'):
                os.remove(f)
