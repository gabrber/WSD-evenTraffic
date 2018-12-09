import os
import matplotlib.pyplot as plt


def create_road_horizontal(a_x, a_y, b_x, b_y):
    step = 0.1
    road_x = []
    road_y = []
    r_x = a_x
    while r_x < b_x:
        r_x = r_x + step
        road_x.append(r_x)
        road_y.append(a_y)

    return road_x, road_y


def create_road_vertical(a_x, a_y, b_x, b_y):
    step = 0.1
    road_x = []
    road_y = []
    r_y = a_y
    while r_y < b_y:
        r_y = r_y + step
        road_y.append(r_y)
        road_x.append(a_x)

    return road_x, road_y


file = open('geometry.txt', 'r')
lines = file.readlines()

nodes_x = []
nodes_y = []
crosses_x = []
crosses_y = []
for i in range(len(lines)):
    line = lines[i].split(' ')
    if i < len(lines)-2:
        nodes_x.append(int(line[1]))
        nodes_y.append(int(line[2][:-1]))
    else:
        crosses_x.append(int(line[1]))
        crosses_y.append(int(line[2][:-1]))

agents_file_names = os.listdir('data/drivers/')
agents_names = [int(x[3:-4]) for x in agents_file_names]

ambulances_file_names = os.listdir('data/ambulances/')
ambulances_names = [int(x[9:-4]) for x in ambulances_file_names]


num_of_frames = len(open('data/drivers/car'+str(agents_names[0])+'.txt', 'r').readlines())
frames = []
for frame in range(num_of_frames):
    agents_in_frame_x = []
    agents_in_frame_y = []
    ambulances_in_frame_x = []
    ambulances_in_frame_y = []
    for name in agents_names:
        file = open('data/drivers/car'+str(name)+'.txt', 'r')
        lines = file.readlines()
        line = lines[frame].split(' ')
        agents_in_frame_x.append(float(line[2]))
        agents_in_frame_y.append(float(line[3]))

    for name in ambulances_names:
        file = open('data/ambulances/ambulance'+str(name)+'.txt', 'r')
        lines = file.readlines()
        line = lines[frame].split(' ')
        ambulances_in_frame_x.append(float(line[2]))
        ambulances_in_frame_y.append(float(line[3]))

    inter1_file = open('data/intersections/inter1.txt', 'r')
    inter1_lines = inter1_file.readlines()
    inter1_line = inter1_lines[frame].split(' ')
    inter1_x = float(inter1_line[2])
    inter1_y = float(inter1_line[3])
    inter1_color = inter1_line[5]

    inter2_file = open('data/intersections/inter2.txt', 'r')
    inter2_lines = inter2_file.readlines()
    inter2_line = inter2_lines[frame].split(' ')
    inter2_x = float(inter2_line[2])
    inter2_y = float(inter2_line[3])
    inter2_color = inter2_line[5]

    frames.append([[agents_in_frame_x, agents_in_frame_y],
                   [ambulances_in_frame_x, ambulances_in_frame_y],
                   [inter1_x, inter1_y, inter1_color],
                   [inter2_x, inter2_y, inter2_color]])

print(frames)
frame_number = 0
for frame in frames:
    plt.figure(figsize=(10,8))
    plt.scatter(x=frame[0][0], y=frame[0][1], c='b', s=45, marker='D')  # drivers
    plt.scatter(x=frame[1][0], y=frame[1][1], c='r', s=80, marker='P')  # ambulances
    plt.scatter(x=frame[2][0], y=frame[2][1], c=frame[2][2][0], s=80, marker='o')  # inter1
    plt.scatter(x=frame[3][0], y=frame[3][1], c=frame[3][2][0], s=80, marker='o')  # inter2
    plt.scatter(x=nodes_x, y=nodes_y, s=20, c='k')
    plt.scatter(x=create_road_horizontal(nodes_x[0], nodes_y[0], nodes_x[5], nodes_y[5])[0],
                y=create_road_horizontal(nodes_x[0], nodes_y[0], nodes_x[5], nodes_y[5])[1],
                s=0.1, c='k')
    plt.scatter(x=create_road_vertical(nodes_x[2], nodes_y[2], nodes_x[1], nodes_y[1])[0],
                y=create_road_vertical(nodes_x[2], nodes_y[2], nodes_x[1], nodes_y[1])[1],
                s=0.1, c='k')
    plt.scatter(x=create_road_vertical(nodes_x[4], nodes_y[4], nodes_x[3], nodes_y[3])[0],
                y=create_road_vertical(nodes_x[4], nodes_y[4], nodes_x[3], nodes_y[3])[1],
                s=0.1, c='k')

    plt.xlim((min(nodes_x) - 5, max(nodes_x) + 5))
    plt.ylim((min(nodes_y) - 5, max(nodes_y) + 5))
    plt.xticks([])
    plt.yticks([])
    # plt.show()
    if frame_number < 10:
        plt.savefig('output/00{}.png'.format(frame_number))
    elif frame_number < 100:
        plt.savefig('output/0{}.png'.format(frame_number))
    else:
        plt.savefig('output/{}.png'.format(frame_number))
    frame_number += 1
    plt.close()

