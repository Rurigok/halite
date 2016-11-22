#!/usr/bin/env python3
import random
import subprocess
import sys, os
from collections import Counter

players = dict()

def err(message):
    print(message)
    print("Usage: {} <# of runs> <bot main files...>".format(sys.argv[0]))
    exit()

def main():
    args = sys.argv[1:]

    runCommands = list()
    runs = -1
    nPlayers = 1

    # Parse # of runs, compile any files and build run command list
    for arg in args:
        if arg.endswith(".java"):
            subprocess.call(["javac", arg])
            runCommands.append("java " + arg[:-5])
            players[nPlayers] = arg[:-5]
            nPlayers += 1
        elif arg.endswith(".py"):
            runCommands.append("python " + arg)
            players[nPlayers] = arg[:-2]
            nPlayers += 1
            continue
        elif arg.isdigit():
            runs = int(arg)
        else:
            err("File type not supported.")

    if runs == -1:
        err("The number of runs must be supplied.")

    print("Compilation finished. Starting simulation(s)...")

    sizes = [x for x in range(20, 55, 5)]

    lines = list()
    results = {}
    runsDone = 0
    try:
        for i in range(runs):
            size = random.choice(sizes)
            #print(os.listdir())
            proc = subprocess.Popen(["./halite", "-q", "-d", "{} {}".format(size, size)] + runCommands, stdout=subprocess.PIPE)
            for line in proc.stdout:
                lines.append(line.decode("utf-8").rstrip())

            print("================ Game {} of {} ================".format(i + 1, runs))
            for line in lines[nPlayers:]:
                if line:
                    playerNum, rank = line.split()

                    playerNum = int(playerNum)
                    rank = int(rank)
                    print("Player #{} {} came in rank #{}".format(playerNum, players[playerNum], rank))
                    player = players[playerNum]
                    rank = 100 - 100 / (nPlayers - 2) * (rank - 1)
                    if player in results:
                        results[player] += rank
                    else:
                        results[player] = rank
            lines = list()
            runsDone += 1
    except KeyboardInterrupt:
        # Cancelled by user! Print results so far...
        if runsDone == 0:
            print("Program ended early by user. No games were finished!")
            exit()

    for player in results:
        results[player] = int(results[player] / (100 * runsDone) * 100)

    print("=================================================")
    print("########## Final Scores (0-100 scale) ###########")

    if runsDone != runs:
        print("Incomplete results: Only {} of {} games were finished!".format(runsDone, runs))

    for player in sorted(results.keys()):
        print('{}: {}'.format(player, results[player]))



if __name__ == '__main__':
    main()
