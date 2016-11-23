#!/usr/bin/env python3
from collections import namedtuple
import multiprocessing as mp
import random
import subprocess
import sys

# Represents a single playerID and rank pair from a single game
Result = namedtuple('Result', 'playerID rank')

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
                rank = 100 - 100 / (self.numPlayers - 1) * (rank - 1)
                # Collect results for each player
                results.append(Result(playerID, rank))
        return results

class SimulationWorker(mp.Process):
    """Worker process that consumes and completes Simulations.

    Continually pulls from a simQueue containing Simulation objects and runs
    them until we encounter a None job, processing each result into the
    resultQueue.
    """

    def __init__(self, simQueue, resultQueue):
        mp.Process.__init__(self)
        self.simQueue = simQueue
        self.resultQueue = resultQueue

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

class Player(object):

    def __init__(self, playerName):
        self.playerName = playerName
        self.totalRank = 0

    def __str__(self):
        return "{} has a total rank of {}".format(self.playerName, self.totalRank)

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
    for arg in sys.argv[1:]:
        if arg.endswith(".java"):
            subprocess.call(["javac", arg])
            runCommands.append("java " + arg[:-5])
            players[playerID] = Player(arg[:-5])
            playerID += 1
        elif arg.endswith(".py"):
            runCommands.append("python " + arg)
            players[playerID] = Player(arg[:-2])
            playerID += 1
            continue
        elif arg.isdigit():
            runs = int(arg)
        else:
            usage("File type not supported.")

    numPlayers = playerID - 1

    if runs == -1:
        usage("The number of runs must be supplied.")

    print("Compilation finished. Starting simulation(s)...")

    # Generate candidate board sizes (only square boards are generated)
    sizes = [x for x in range(20, 55, 5)]

    # Create input/output queues
    simQueue = mp.JoinableQueue()
    resultQueue = mp.Queue()
    jobs = []

    # Create and start simulation worker processes
    numWorkers = mp.cpu_count()
    workers = [ SimulationWorker(simQueue, resultQueue)
                for i in range(numWorkers) ]
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
        players[result.playerID].totalRank += result.rank

    print("=================================================")
    print("########## Final Scores (0-100 scale) ###########")

    for player in players.values():
        score = int(player.totalRank / (100 * runs) * 100)
        print("{}: {}".format(player.playerName, score))

if __name__ == '__main__':
    main()
