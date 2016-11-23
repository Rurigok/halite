import json, sys
from pprint import pprint


if __name__ == '__main__':
    frames = []

    f = open('{}'.format(sys.argv[1]), 'r')

    for frame_raw in f.read().split('\n\n'):
        frame = []

        for line in frame_raw.split('\n'):

            col = [float(val) for val in line.split()]
            frame.append(col)



        frames.append(frame)

    out = open('fin.json', 'w')
    json.dump(frames, out)
    out.close()