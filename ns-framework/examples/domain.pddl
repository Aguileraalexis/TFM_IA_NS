(define (domain appointments)
  (:requirements :strips :typing)
  (:types user slot)
  (:predicates
    (registered ?u - user)
    (slot-available ?s - slot)
    (appointment-booked ?u - user ?s - slot)
  )

  (:action register-user
    :parameters (?u - user)
    :precondition (not (registered ?u))
    :effect (registered ?u)
  )

  (:action book-appointment
    :parameters (?u - user ?s - slot)
    :precondition (and (registered ?u) (slot-available ?s))
    :effect (and (appointment-booked ?u ?s)
                 (not (slot-available ?s)))
  )
)
