package com.skynet.service;

import com.skynet.model.Ticket;

import java.util.Comparator;

public class TicketPriorityComparator implements Comparator<Ticket> {
    @Override
    public int compare(Ticket a, Ticket b) {
        int byTier = Integer.compare(a.getPassenger().getTier().getRank(), b.getPassenger().getTier().getRank());
        if (byTier != 0) {
            return byTier;
        }
        int byClass = Integer.compare(a.getFareClass().getRank(), b.getFareClass().getRank());
        if (byClass != 0) {
            return byClass;
        }
        return a.compareTo(b);
    }
}
